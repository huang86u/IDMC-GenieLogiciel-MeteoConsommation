package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatistiqueServiceTest {

    private DonneesMeteoRepository meteoRepository;
    private StatistiqueService statistiqueService;

    @BeforeEach
    void setUp() {
        meteoRepository = mock(DonneesMeteoRepository.class);
        statistiqueService = new StatistiqueService(meteoRepository);
    }

    @Test
    void calculerCorrelationRetourneUnPourCorrelationPositiveParfaite() {
        double correlation = statistiqueService.calculerCorrelation(
                List.of(1.0, 2.0, 3.0),
                List.of(10.0, 20.0, 30.0)
        );

        assertEquals(1.0, correlation, 0.0001);
    }

    @Test
    void calculerCorrelationIgnoreLesPairesAvecValeurNulle() {
        double correlation = statistiqueService.calculerCorrelation(
                Arrays.asList(1.0, null, 3.0),
                List.of(10.0, 20.0, 30.0)
        );

        assertEquals(1.0, correlation, 0.0001);
    }

    @Test
    void calculerCorrelationRetourneZeroSiTaillesDifferentes() {
        double correlation = statistiqueService.calculerCorrelation(
                List.of(1.0, 2.0),
                List.of(10.0)
        );

        assertEquals(0.0, correlation);
    }

    @Test
    void analyserDonneesCalculePlusieursIndicateurs() {
        when(meteoRepository.findAnalysePoints()).thenReturn(List.of(
                new AnalysePoint(LocalDate.of(2014, 1, 1), LocalTime.of(0, 0), 1.0, 80.0, 2.0, 10.0, 100.0),
                new AnalysePoint(LocalDate.of(2014, 1, 1), LocalTime.of(1, 0), 2.0, 70.0, 1.0, 20.0, 200.0),
                new AnalysePoint(LocalDate.of(2014, 2, 1), LocalTime.of(0, 0), 3.0, 60.0, 0.0, 30.0, 300.0)
        ));

        AnalyseResult resultat = statistiqueService.analyserDonnees();

        assertEquals(3, resultat.nombrePoints());
        assertEquals(1.0, resultat.correlations().temperature(), 0.0001);
        assertEquals(-1.0, resultat.correlations().humidite(), 0.0001);
        assertEquals(150.0, resultat.consommationMoyenneParMois().get(1), 0.0001);
        assertEquals(200.0, resultat.consommationMoyenneParHeure().get(0), 0.0001);
    }
}
