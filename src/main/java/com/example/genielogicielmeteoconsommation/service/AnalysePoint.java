package com.example.genielogicielmeteoconsommation.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record AnalysePoint(
        LocalDate date,
        LocalTime heure,
        Double temperature,
        Double humidite,
        Double precipitations,
        Double vent,
        Double consommationMw
) {
}
