package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.service.DashboardService;
import com.example.genielogicielmeteoconsommation.service.ImportDonneesService;
import com.example.genielogicielmeteoconsommation.service.ImportMeteoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
public class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportDonneesService importDonneesService;

    @MockBean
    private ImportMeteoService importMeteoService;

    @MockBean
    private DashboardService dashboardService;

    @Test
    public void testImporterElectricite_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
        given(importDonneesService.importerFichierRte(any()))
                .willReturn(new ImportSummary("electricite", "test.csv", 12, 2));

        mockMvc.perform(multipart("/api/donnees/importer-electricite").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Import electricite termine : 12 lignes valides ajoutees, 2 lignes ignorees."));
    }

    @Test
    public void testImporterMeteo_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "meteo1.csv", "text/csv", "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "meteo2.csv", "text/csv", "data".getBytes());
        given(importMeteoService.importerFichierMeteo(any()))
                .willReturn(new ImportSummary("meteo", "meteo.csv", 20, 4));

        mockMvc.perform(multipart("/api/donnees/importer-meteo")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(content().string("Import meteo termine : 2 fichiers traites, 40 lignes valides ajoutees, 8 lignes ignorees."));
    }
}
