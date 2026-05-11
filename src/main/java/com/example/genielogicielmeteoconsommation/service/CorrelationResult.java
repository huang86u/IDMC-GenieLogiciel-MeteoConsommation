package com.example.genielogicielmeteoconsommation.service;

public record CorrelationResult(
        double temperature,
        double humidite,
        double precipitations,
        double vent
) {
}
