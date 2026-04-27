package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import com.example.genielogicielmeteoconsommation.support.GrandEstReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class DatasetBootstrapService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetBootstrapService.class);
    private static final String REGIONAL_DATASET_NAME = "eco2mix-regional-cons-def.csv";
    private static final Set<String> DEPARTMENT_CODES = GrandEstReference.DEPARTMENTS.keySet();

    private final ConsommationElectriqueRepository consommationRepository;
    private final DonneesMeteoRepository meteoRepository;
    private final ImportDonneesService importDonneesService;
    private final ImportMeteoService importMeteoService;
    private final boolean bootstrapEnabled;
    private final String datasetZipPath;

    public DatasetBootstrapService(
            ConsommationElectriqueRepository consommationRepository,
            DonneesMeteoRepository meteoRepository,
            ImportDonneesService importDonneesService,
            ImportMeteoService importMeteoService,
            @Value("${app.dataset.bootstrap.enabled:true}") boolean bootstrapEnabled,
            @Value("${app.dataset.zip-path:}") String datasetZipPath
    ) {
        this.consommationRepository = consommationRepository;
        this.meteoRepository = meteoRepository;
        this.importDonneesService = importDonneesService;
        this.importMeteoService = importMeteoService;
        this.bootstrapEnabled = bootstrapEnabled;
        this.datasetZipPath = datasetZipPath;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapIfNeeded();
    }

    void bootstrapIfNeeded() {
        if (!bootstrapEnabled) {
            LOGGER.info("Chargement automatique du dataset desactive.");
            return;
        }

        if (datasetZipPath == null || datasetZipPath.isBlank()) {
            LOGGER.info("Aucun zip de dataset configure pour le chargement automatique.");
            return;
        }

        Path zipPath = Path.of(datasetZipPath);
        if (!Files.exists(zipPath)) {
            LOGGER.warn("Zip de dataset introuvable: {}", zipPath);
            return;
        }

        boolean hasElectricityData = consommationRepository.count() > 0;
        boolean hasWeatherData = meteoRepository.count() > 0;

        if (hasElectricityData && hasWeatherData) {
            LOGGER.info("Les donnees sont deja presentes en base. Aucun rechargement automatique.");
            return;
        }

        LOGGER.info("Initialisation backend des donnees depuis {}", zipPath);

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            if (!hasElectricityData) {
                importRegionalDataset(zipFile);
            } else {
                LOGGER.info("Consommation deja presente: import regional ignore.");
            }

            if (!hasWeatherData) {
                importWeatherDatasets(zipFile);
            } else {
                LOGGER.info("Meteo deja presente: import departemental ignore.");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'initialiser les donnees depuis le zip backend.", exception);
        }
    }

    private void importRegionalDataset(ZipFile zipFile) throws Exception {
        ZipEntry regionalEntry = zipFile.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(REGIONAL_DATASET_NAME))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Fichier regional introuvable dans le zip."));

        try (InputStream inputStream = zipFile.getInputStream(regionalEntry)) {
            ImportSummary summary = importDonneesService.importerFluxRte(regionalEntry.getName(), inputStream);
            LOGGER.info(
                    "Import regional termine: {} lignes ajoutees, {} lignes ignorees.",
                    summary.insertedRows(),
                    summary.skippedRows()
            );
        }
    }

    private void importWeatherDatasets(ZipFile zipFile) throws Exception {
        List<? extends ZipEntry> weatherEntries = zipFile.stream()
                .filter(this::isWeatherDataset)
                .sorted(Comparator.comparing(ZipEntry::getName))
                .toList();

        if (weatherEntries.isEmpty()) {
            throw new IllegalStateException("Aucun fichier meteo departemental n'a ete trouve dans le zip.");
        }

        int insertedRows = 0;
        int skippedRows = 0;

        for (ZipEntry weatherEntry : weatherEntries) {
            try (InputStream inputStream = zipFile.getInputStream(weatherEntry)) {
                ImportSummary summary = importMeteoService.importerFluxMeteo(weatherEntry.getName(), inputStream);
                insertedRows += summary.insertedRows();
                skippedRows += summary.skippedRows();
                LOGGER.info(
                        "Import meteo {}: {} lignes ajoutees, {} lignes ignorees.",
                        weatherEntry.getName(),
                        summary.insertedRows(),
                        summary.skippedRows()
                );
            }
        }

        LOGGER.info(
                "Import meteo backend termine: {} fichiers, {} lignes ajoutees, {} lignes ignorees.",
                weatherEntries.size(),
                insertedRows,
                skippedRows
        );
    }

    private boolean isWeatherDataset(ZipEntry entry) {
        if (entry.isDirectory()) {
            return false;
        }

        String name = entry.getName();
        if (!name.endsWith(".csv") || !name.contains("/H_") || !name.contains("_2010-2019")) {
            return false;
        }

        String fileName = Path.of(name).getFileName().toString();
        if (fileName.length() < 4) {
            return false;
        }

        String departmentCode = fileName.substring(2, 4);
        return DEPARTMENT_CODES.contains(departmentCode);
    }
}
