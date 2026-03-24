package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DatasetBootstrapServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private ConsommationElectriqueRepository consommationRepository;

    @Mock
    private DonneesMeteoRepository meteoRepository;

    @Mock
    private ImportDonneesService importDonneesService;

    @Mock
    private ImportMeteoService importMeteoService;

    @Test
    void bootstrapIfNeededShouldImportWhenDatabaseIsEmpty() throws Exception {
        Path zipPath = tempDir.resolve("dataset.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("DatasetGenieLogiciel/Donnee regional/eco2mix-regional-cons-def.csv"));
            zipOutputStream.write("header\n".getBytes());
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("DatasetGenieLogiciel/DatasetGenieLogiciel/H_08_2010-2019.csv"));
            zipOutputStream.write("header\n".getBytes());
            zipOutputStream.closeEntry();
        }

        given(consommationRepository.count()).willReturn(0L);
        given(meteoRepository.count()).willReturn(0L);
        given(importDonneesService.importerFluxRte(eq("DatasetGenieLogiciel/Donnee regional/eco2mix-regional-cons-def.csv"), any(InputStream.class)))
                .willReturn(new ImportSummary("electricite", "regional.csv", 10, 2));
        given(importMeteoService.importerFluxMeteo(eq("DatasetGenieLogiciel/DatasetGenieLogiciel/H_08_2010-2019.csv"), any(InputStream.class)))
                .willReturn(new ImportSummary("meteo", "H_08.csv", 20, 4));

        DatasetBootstrapService service = new DatasetBootstrapService(
                consommationRepository,
                meteoRepository,
                importDonneesService,
                importMeteoService,
                true,
                zipPath.toString()
        );

        service.bootstrapIfNeeded();

        verify(importDonneesService).importerFluxRte(
                eq("DatasetGenieLogiciel/Donnee regional/eco2mix-regional-cons-def.csv"),
                any(InputStream.class)
        );
        verify(importMeteoService).importerFluxMeteo(
                eq("DatasetGenieLogiciel/DatasetGenieLogiciel/H_08_2010-2019.csv"),
                any(InputStream.class)
        );
    }
}
