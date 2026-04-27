package com.example.genielogicielmeteoconsommation.dto;

import java.util.Map;

public record EstimateResponse(
        boolean modelReady,
        String message,
        Double estimatedConsumptionMw,
        Double intercept,
        Map<String, Double> coefficients,
        Map<String, Double> inputsUsed,
        Double rSquared,
        Long observationsUsed
) {
}
