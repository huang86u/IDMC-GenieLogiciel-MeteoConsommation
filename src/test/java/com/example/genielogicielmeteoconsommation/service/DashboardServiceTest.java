package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.DashboardOverviewResponse;
import com.example.genielogicielmeteoconsommation.dto.EstimateRequest;
import com.example.genielogicielmeteoconsommation.dto.EstimateResponse;
import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ConsommationElectriqueRepository consommationRepository;

    @Mock
    private DonneesMeteoRepository meteoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void buildOverviewAndEstimateProvideMeaningfulResults() {
        List<ConsommationElectrique> consommations = List.of(
                consommation("Alsace", "2014-01-05", 12, 5000), consommation("Lorraine", "2014-01-05", 12, 4200),
                consommation("Alsace", "2014-02-10", 12, 4900), consommation("Lorraine", "2014-02-10", 12, 4100),
                consommation("Alsace", "2014-04-02", 12, 3800), consommation("Lorraine", "2014-04-02", 12, 3000),
                consommation("Alsace", "2014-05-10", 12, 3600), consommation("Lorraine", "2014-05-10", 12, 2900),
                consommation("Alsace", "2014-07-03", 12, 3000), consommation("Lorraine", "2014-07-03", 12, 2400),
                consommation("Alsace", "2014-08-18", 12, 3200), consommation("Lorraine", "2014-08-18", 12, 2500),
                consommation("Alsace", "2014-10-08", 12, 4100), consommation("Lorraine", "2014-10-08", 12, 3200),
                consommation("Alsace", "2014-11-20", 12, 4500), consommation("Lorraine", "2014-11-20", 12, 3600)
        );

        List<DonneesMeteo> meteoRows = List.of(
                meteo("67", "6700001", "2014-01-05", 12, 0, 86, 20, 1.3),
                meteo("67", "6700001", "2014-02-10", 12, 1, 82, 18, 0.8),
                meteo("67", "6700001", "2014-04-02", 12, 12, 70, 14, 0.4),
                meteo("67", "6700001", "2014-05-10", 12, 15, 65, 13, 0.1),
                meteo("67", "6700001", "2014-07-03", 12, 25, 56, 9, 0.0),
                meteo("67", "6700001", "2014-08-18", 12, 24, 58, 8, 0.0),
                meteo("67", "6700001", "2014-10-08", 12, 11, 75, 12, 0.9),
                meteo("67", "6700001", "2014-11-20", 12, 5, 80, 15, 1.1)
        );

        given(consommationRepository.findAllByDateBetween(any(), any())).willReturn(consommations);
        given(meteoRepository.findAllByDateBetweenAndDepartementIn(any(), any(), any())).willReturn(meteoRows);

        DashboardOverviewResponse overview = dashboardService.buildOverview(
                List.of("67"),
                LocalDate.of(2014, 1, 1),
                LocalDate.of(2014, 12, 31)
        );

        assertTrue(overview.summary().dataReady());
        assertFalse(overview.dailyTrends().isEmpty());
        assertEquals(8, overview.coverage().joinedHourlyObservations());
        assertEquals(4, overview.seasonalComparisons().size());
        assertTrue(overview.summary().correlationTemperatureConsumption() < 0);
        assertNotNull(overview.regressionModel());
        assertTrue(overview.regressionModel().ready());

        EstimateResponse estimate = dashboardService.estimate(new EstimateRequest(
                List.of("67"),
                LocalDate.of(2014, 1, 1),
                LocalDate.of(2014, 12, 31),
                6.0,
                78.0,
                14.0,
                1.0
        ));

        assertTrue(estimate.modelReady());
        assertNotNull(estimate.estimatedConsumptionMw());
        assertTrue(estimate.estimatedConsumptionMw() > 0);
        verify(consommationRepository, times(1)).findAllByDateBetween(any(), any());
        verify(meteoRepository, times(1)).findAllByDateBetweenAndDepartementIn(any(), any(), any());
    }

    private ConsommationElectrique consommation(String region, String date, int hour, double value) {
        ConsommationElectrique consommation = new ConsommationElectrique();
        consommation.setRegion(region);
        consommation.setDate(LocalDate.parse(date));
        consommation.setHeure(LocalTime.of(hour, 0));
        consommation.setConsommationMw(value);
        return consommation;
    }

    private DonneesMeteo meteo(
            String departement,
            String station,
            String date,
            int hour,
            double temperature,
            double humidity,
            double wind,
            double rain
    ) {
        DonneesMeteo meteo = new DonneesMeteo();
        meteo.setDepartement(departement);
        meteo.setStation(station);
        meteo.setDate(LocalDate.parse(date));
        meteo.setHeure(LocalTime.of(hour, 0));
        meteo.setTemperature(temperature);
        meteo.setHumidite(humidity);
        meteo.setVent(wind);
        meteo.setPrecipitations(rain);
        return meteo;
    }
}
