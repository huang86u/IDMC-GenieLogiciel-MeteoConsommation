package com.example.genielogicielmeteoconsommation.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DonneesMeteoTest {

    @Test
    public void testGettersAndSetters() {
        DonneesMeteo meteo = new DonneesMeteo();
        meteo.setId(10L);
        meteo.setDepartement("08");
        meteo.setStation("08001001");
        meteo.setDate(LocalDate.of(2014, 2, 10));
        meteo.setHeure(LocalTime.of(14, 30));
        meteo.setTemperature(15.5);
        meteo.setHumidite(80.0);
        meteo.setPrecipitations(2.0);
        meteo.setVent(25.0);

        assertEquals(10L, meteo.getId());
        assertEquals("08", meteo.getDepartement());
        assertEquals("08001001", meteo.getStation());
        assertEquals(LocalDate.of(2014, 2, 10), meteo.getDate());
        assertEquals(LocalTime.of(14, 30), meteo.getHeure());
        assertEquals(15.5, meteo.getTemperature());
        assertEquals(80.0, meteo.getHumidite());
        assertEquals(2.0, meteo.getPrecipitations());
        assertEquals(25.0, meteo.getVent());
    }
}