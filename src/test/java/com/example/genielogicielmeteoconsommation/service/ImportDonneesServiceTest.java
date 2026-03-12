package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ImportDonneesServiceTest {

    @Mock
    private ConsommationElectriqueRepository repository;

    @InjectMocks
    private ImportDonneesService importService;

    @Test
    public void testImporterFichierRte() throws Exception {
        // Préparation du faux fichier CSV
        String csvContent = "Périmètre;Nature;Date;Heures;Consommation\n" +
                "Grand Est;Def;01/01/2014;12:00;5500.5\n" + // Ligne valide
                "Bretagne;Def;01/01/2014;12:00;3000\n" +    // Ligne ignorée (hors région)
                "Grand Est;Def;02/01/2014;13:00;ND\n";      // Ligne ignorée (valeur ND)

        MockMultipartFile mockFile = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes());

        // Exécution
        importService.importerFichierRte(mockFile);

        // Vérification
        ArgumentCaptor<List<ConsommationElectrique>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ConsommationElectrique> savedList = captor.getValue();

        // Seule 1 ligne doit être sauvegardée
        assertEquals(1, savedList.size());
        assertEquals("Grand Est", savedList.get(0).getRegion());
        assertEquals(5500.5, savedList.get(0).getConsommationMw());
        assertEquals("2014-01-01", savedList.get(0).getDate().toString());
    }
}