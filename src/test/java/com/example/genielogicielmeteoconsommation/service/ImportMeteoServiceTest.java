package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ImportMeteoServiceTest {

    @Mock
    private DonneesMeteoRepository meteoRepository;

    @InjectMocks
    private ImportMeteoService importMeteoService;

    @Test
    void testImporterFichierMeteo() {
        String[] columns = new String[80];
        Arrays.fill(columns, "");

        columns[0] = "08001001";
        columns[5] = "2014010112";
        columns[6] = "1.5";
        columns[10] = "25.0";
        columns[42] = "10.2";
        columns[76] = "80";

        String header = "NUM_POSTE;NOM_USUEL;LAT;LON;ALTI;AAAAMMJJHH;RR1;QRR1;DRR1;QDRR1;FF;QFF\n";
        String validLine = String.join(";", columns) + "\n";

        columns[5] = "2015010112";
        String invalidLine = String.join(";", columns) + "\n";

        String csvContent = header + validLine + invalidLine;
        MockMultipartFile mockFile = new MockMultipartFile("files", "meteo.csv", "text/csv", csvContent.getBytes());

        importMeteoService.importerFichierMeteo(mockFile);

        ArgumentCaptor<List<DonneesMeteo>> captor = ArgumentCaptor.forClass(List.class);
        verify(meteoRepository).saveAll(captor.capture());

        List<DonneesMeteo> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        assertEquals("08", savedList.get(0).getDepartement());
        assertEquals("13:00", savedList.get(0).getHeure().toString());
        assertEquals(10.2, savedList.get(0).getTemperature());
    }
}
