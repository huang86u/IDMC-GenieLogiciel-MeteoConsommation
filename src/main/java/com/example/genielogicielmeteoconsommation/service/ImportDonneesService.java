package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import com.example.genielogicielmeteoconsommation.support.GrandEstReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ImportDonneesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDonneesService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int BATCH_SIZE = 1_000;

    private final ConsommationElectriqueRepository repository;

    public ImportDonneesService(ConsommationElectriqueRepository repository) {
        this.repository = repository;
    }

    public ImportSummary importerFichierRte(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier RTE est vide.");
        }

        try {
            return importerFluxRte(file.getOriginalFilename(), file.getInputStream());
        } catch (Exception exception) {
            throw new RuntimeException("Erreur lors de la lecture du fichier CSV RTE", exception);
        }
    }

    public ImportSummary importerFluxRte(String sourceName, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Le flux RTE est introuvable.");
        }

        List<ConsommationElectrique> rowsToSave = new ArrayList<>(BATCH_SIZE);
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
                if (columns.length < 7) {
                    skippedRows++;
                    continue;
                }

                String region = columns[1].trim();
                if (!GrandEstReference.IMPORT_REGIONS.contains(region)) {
                    skippedRows++;
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(columns[3].trim(), DATE_FORMATTER);
                    LocalTime time = parseTime(columns[4].trim());
                    if ("24:00".equals(columns[4].trim())) {
                        date = date.plusDays(1);
                    }

                    if (date.isBefore(GrandEstReference.STUDY_START_DATE)
                            || date.isAfter(GrandEstReference.STUDY_END_DATE)) {
                        skippedRows++;
                        continue;
                    }

                    Double consumption = parseNumber(columns[6]);
                    if (consumption == null) {
                        skippedRows++;
                        continue;
                    }

                    String rowKey = region + "|" + date + "|" + time;
                    if (!uniqueRows.add(rowKey)) {
                        skippedRows++;
                        continue;
                    }

                    ConsommationElectrique record = new ConsommationElectrique();
                    record.setRegion(region);
                    record.setDate(date);
                    record.setHeure(time);
                    record.setConsommationMw(consumption);
                    rowsToSave.add(record);

                    insertedRows += flushBatchIfNeeded(rowsToSave);
                } catch (Exception exception) {
                    skippedRows++;
                    LOGGER.debug("Ligne RTE ignoree: {}", line, exception);
                }
            }

            insertedRows += flushBatch(rowsToSave);
            return new ImportSummary("electricite", safeSourceName(sourceName), insertedRows, skippedRows);
        } catch (Exception exception) {
            throw new RuntimeException("Erreur lors de la lecture du flux RTE", exception);
        }
    }

    private int flushBatchIfNeeded(List<ConsommationElectrique> rowsToSave) {
        if (rowsToSave.size() < BATCH_SIZE) {
            return 0;
        }
        return flushBatch(rowsToSave);
    }

    private int flushBatch(List<ConsommationElectrique> rowsToSave) {
        if (rowsToSave.isEmpty()) {
            return 0;
        }

        int batchSize = rowsToSave.size();
        repository.saveAll(new ArrayList<>(rowsToSave));
        rowsToSave.clear();
        return batchSize;
    }

    private LocalTime parseTime(String timeValue) {
        if ("24:00".equals(timeValue)) {
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(timeValue);
    }

    private Double parseNumber(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "ND".equalsIgnoreCase(normalized) || "-".equals(normalized)) {
            return null;
        }

        return Double.parseDouble(normalized.replace(",", "."));
    }

    private String safeSourceName(String sourceName) {
        return sourceName == null || sourceName.isBlank() ? "donnees-rte.csv" : sourceName;
    }
}
