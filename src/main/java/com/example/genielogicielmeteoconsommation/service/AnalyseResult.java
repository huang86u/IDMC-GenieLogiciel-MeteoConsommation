package com.example.genielogicielmeteoconsommation.service;

import java.util.Map;

public record AnalyseResult(
        int nombrePoints,
        CorrelationResult correlations,
        Map<Integer, Double> consommationMoyenneParMois,
        Map<Integer, Double> consommationMoyenneParHeure
) {
}
