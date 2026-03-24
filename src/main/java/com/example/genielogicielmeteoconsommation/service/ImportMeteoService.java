package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import com.example.genielogicielmeteoconsommation.support.GrandEstReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportMeteoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportMeteoService.class);
    private static final DateTimeFormatter METEO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
    private static final int BATCH_SIZE = 5_000;

    private final DonneesMeteoRepository meteoRepository;

    public ImportMeteoService(DonneesMeteoRepository meteoRepository) {
        this.meteoRepository = meteoRepository;
    }

    public ImportSummary importerFichierMeteo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier meteo est vide.");
        }

        try {
            return importerFluxMeteo(file.getOriginalFilename(), file.getInputStream());
        } catch (Exception exception) {
            throw new RuntimeException("Erreur lors de la lecture du fichier meteo", exception);
        }
    }

    public ImportSummary importerFluxMeteo(String sourceName, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Le flux meteo est introuvable.");
        }

        List<DonneesMeteo> rowsToSave = new ArrayList<>(BATCH_SIZE);
        Set<String> uniqueRows = new HashSet<>();
        int skippedRows = 0;
        int insertedRows = 0;

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = bufferedReader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] columns = line.split(";");
                if (columns.length < 77) {
                    skippedRows++;
                    continue;
                }

                try {
                    String station = columns[0].trim();
                    String department = station.length() >= 2 ? station.substring(0, 2) : "XX";
                    String dateTimeValue = columns[5].trim();

                    if (!GrandEstReference.DEPARTMENTS.containsKey(department) || !dateTimeValue.startsWith("2014")) {
                        skippedRows++;
                        continue;
                    }

                    LocalDateTime parsedDateTime = LocalDateTime.parse(dateTimeValue, METEO_FORMATTER);
                    ZonedDateTime parisTime = parsedDateTime.atZone(UTC_ZONE).withZoneSameInstant(PARIS_ZONE);

                    if (parisTime.toLocalDate().isBefore(GrandEstReference.STUDY_START_DATE)
                            || parisTime.toLocalDate().isAfter(GrandEstReference.STUDY_END_DATE)) {
                        skippedRows++;
                        continue;
                    }

                    String rowKey = station + "|" + parisTime.toLocalDate() + "|" + parisTime.toLocalTime();
                    if (!uniqueRows.add(rowKey)) {
                        skippedRows++;
                        continue;
                    }

                    DonneesMeteo record = new DonneesMeteo();
                    record.setDepartement(department);
                    record.setStation(station);
                    record.setDate(parisTime.toLocalDate());
                    record.setHeure(parisTime.toLocalTime());
                    record.setPrecipitations(parseValue(columns[6]));
                    record.setVent(parseValue(columns[10]));
                    record.setTemperature(parseValue(columns[42]));
                    record.setHumidite(parseValue(columns[76]));
                    rowsToSave.add(record);

                    insertedRows += flushBatchIfNeeded(rowsToSave);
                } catch (Exception exception) {
                    skippedRows++;
                    LOGGER.debug("Ligne meteo ignoree: {}", line, exception);
                }
            }

            insertedRows += flushBatch(rowsToSave);
            return new ImportSummary("meteo", safeSourceName(sourceName), insertedRows, skippedRows);
        } catch (Exception exception) {
            throw new RuntimeException("Erreur lors de la lecture du flux meteo", exception);
        }
    }

    private int flushBatchIfNeeded(List<DonneesMeteo> rowsToSave) {
        if (rowsToSave.size() < BATCH_SIZE) {
            return 0;
        }
        return flushBatch(rowsToSave);
    }

    private int flushBatch(List<DonneesMeteo> rowsToSave) {
        if (rowsToSave.isEmpty()) {
            return 0;
        }

        int batchSize = rowsToSave.size();
        meteoRepository.saveAll(new ArrayList<>(rowsToSave));
        rowsToSave.clear();
        return batchSize;
    }

    private Double parseValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "ND".equalsIgnoreCase(normalized)) {
            return null;
        }

        try {
            return Double.parseDouble(normalized.replace(",", "."));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String safeSourceName(String sourceName) {
        return sourceName == null || sourceName.isBlank() ? "donnees-meteo.csv" : sourceName;
    }
}
