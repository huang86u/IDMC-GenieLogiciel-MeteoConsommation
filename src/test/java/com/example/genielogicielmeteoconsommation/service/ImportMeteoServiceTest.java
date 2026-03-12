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
public class ImportMeteoServiceTest {

    @Mock
    private DonneesMeteoRepository meteoRepository;

    @InjectMocks
    private ImportMeteoService importMeteoService;

    @Test
    public void testImporterFichierMeteo() throws Exception {
        // Construction d'une ligne de 80 colonnes vides pour éviter l'IndexOutOfBounds
        String[] colonnes = new String[80];
        Arrays.fill(colonnes, "");

        // Remplissage avec des données valides pour l'année 2014 (Indices : 0, 5, 6, 10, 42, 76)
        colonnes[0] = "08001001";     // NUM_POSTE
        colonnes[5] = "2014010112";   // AAAAMMJJHH (12h UTC = 13h Paris en hiver)
        colonnes[6] = "1.5";          // RR1
        colonnes[10] = "25.0";        // FF
        colonnes[42] = "10.2";        // T
        colonnes[76] = "80";          // U

        String enTete = "NUM_POSTE;NOM_USUEL;LAT;LON;ALTI;AAAAMMJJHH;RR1;QRR1;DRR1;QDRR1;FF;QFF;... (et autres colonnes)\n";
        String ligneValide = String.join(";", colonnes) + "\n";

        // Ligne ignorée car pas de l'année 2014
        colonnes[5] = "2015010112";
        String ligneInvalide = String.join(";", colonnes) + "\n";

        String csvContent = enTete + ligneValide + ligneInvalide;
        MockMultipartFile mockFile = new MockMultipartFile("files", "meteo.csv", "text/csv", csvContent.getBytes());

        // Exécution
        importMeteoService.importerFichierMeteo(mockFile);

        // Vérification
        ArgumentCaptor<List<DonneesMeteo>> captor = ArgumentCaptor.forClass(List.class);
        verify(meteoRepository).saveAll(captor.capture());

        List<DonneesMeteo> savedList = captor.getValue();

        assertEquals(1, savedList.size());
        assertEquals("08", savedList.get(0).getDepartement());
        assertEquals("13:00", savedList.get(0).getHeure().toString()); // Test de la conversion UTC -> Europe/Paris
        assertEquals(10.2, savedList.get(0).getTemperature());
    }
}