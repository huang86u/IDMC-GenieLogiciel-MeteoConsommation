package com.example.genielogicielmeteoconsommation.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsommationElectriqueTest {

    @Test
    public void testGettersAndSetters() {
        ConsommationElectrique conso = new ConsommationElectrique();
        conso.setId(1L);
        conso.setRegion("Grand Est");
        conso.setDate(LocalDate.of(2014, 1, 1));
        conso.setHeure(LocalTime.of(12, 0));
        conso.setConsommationMw(5500.5);

        assertEquals(1L, conso.getId());
        assertEquals("Grand Est", conso.getRegion());
        assertEquals(LocalDate.of(2014, 1, 1), conso.getDate());
        assertEquals(LocalTime.of(12, 0), conso.getHeure());
        assertEquals(5500.5, conso.getConsommationMw());
    }
}