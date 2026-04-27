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
class ImportDonneesServiceTest {

    @Mock
    private ConsommationElectriqueRepository repository;

    @InjectMocks
    private ImportDonneesService importService;

    @Test
    void testImporterFichierRte() {
        String csvContent = "Code INSEE region;Region;Nature;Date;Heure;Date - Heure;Consommation (MW)\n"
                + "53;Bretagne;Def;01/01/2014;12:00;2014-01-01T12:00:00+00:00;3000\n"
                + "44;Grand Est;Def;01/01/2014;12:00;2014-01-01T12:00:00+00:00;5500.5\n"
                + "44;Grand Est;Def;02/01/2014;13:00;2014-01-02T13:00:00+00:00;ND\n";

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "eco2mix-regional-cons-def.csv",
                "text/csv",
                csvContent.getBytes()
        );

        importService.importerFichierRte(mockFile);

        ArgumentCaptor<List<ConsommationElectrique>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<ConsommationElectrique> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        assertEquals("Grand Est", savedList.get(0).getRegion());
        assertEquals(5500.5, savedList.get(0).getConsommationMw());
        assertEquals("2014-01-01", savedList.get(0).getDate().toString());
        assertEquals("12:00", savedList.get(0).getHeure().toString());
    }
}
