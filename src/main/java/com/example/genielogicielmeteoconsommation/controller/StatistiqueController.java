package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.service.AnalyseResult;
import com.example.genielogicielmeteoconsommation.service.StatistiqueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistiques")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    public StatistiqueController(StatistiqueService statistiqueService) {
        this.statistiqueService = statistiqueService;
    }

    @GetMapping("/analyse")
    public AnalyseResult analyserDonnees() {
        return statistiqueService.analyserDonnees();
    }

    @GetMapping("/correlation-temperature")
    public double obtenirCorrelationTemperature() {
        return statistiqueService.obtenirCorrelationFinale();
    }
}
