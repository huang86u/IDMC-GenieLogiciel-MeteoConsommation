package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.service.ImportDonneesService;
import com.example.genielogicielmeteoconsommation.service.ImportMeteoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    public void testImporterElectricite_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());

        mockMvc.perform(multipart("/api/donnees/importer-electricite").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("Fichier RTE importé et filtré avec succès !"));
    }

    @Test
    public void testImporterMeteo_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "meteo1.csv", "text/csv", "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "meteo2.csv", "text/csv", "data".getBytes());

        mockMvc.perform(multipart("/api/donnees/importer-meteo")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(content().string("2 fichier(s) Météo importé(s) avec succès !"));
    }
}