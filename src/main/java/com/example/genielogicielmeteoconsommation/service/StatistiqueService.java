package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatistiqueService {

    private final DonneesMeteoRepository meteoRepository;

    public StatistiqueService(DonneesMeteoRepository meteoRepository) {
        this.meteoRepository = meteoRepository;
    }

    public double obtenirCorrelationFinale() {
        return analyserDonnees().correlations().temperature();
    }

    public AnalyseResult analyserDonnees() {
        List<AnalysePoint> points = meteoRepository.findAnalysePoints();

        if (points.isEmpty()) {
            return new AnalyseResult(
                    0,
                    new CorrelationResult(0.0, 0.0, 0.0, 0.0),
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }

        List<Double> temperatures = points.stream()
                .map(AnalysePoint::temperature)
                .toList();
        List<Double> humidites = points.stream()
                .map(AnalysePoint::humidite)
                .toList();
        List<Double> precipitations = points.stream()
                .map(AnalysePoint::precipitations)
                .toList();
        List<Double> vents = points.stream()
                .map(AnalysePoint::vent)
                .toList();
        List<Double> consommations = points.stream()
                .map(AnalysePoint::consommationMw)
                .toList();

        CorrelationResult correlations = new CorrelationResult(
                calculerCorrelation(temperatures, consommations),
                calculerCorrelation(humidites, consommations),
                calculerCorrelation(precipitations, consommations),
                calculerCorrelation(vents, consommations)
        );

        return new AnalyseResult(
                points.size(),
                correlations,
                calculerConsommationMoyenne(points, point -> point.date().getMonthValue()),
                calculerConsommationMoyenne(points, point -> point.heure().getHour())
        );
    }

    public double calculerCorrelation(List<Double> temperatures, List<Double> consommations) {
        if (temperatures == null || consommations == null || temperatures.size() != consommations.size()) {
            return 0.0;
        }

        List<Double> temperaturesValides = new ArrayList<>();
        List<Double> consommationsValides = new ArrayList<>();

        for (int i = 0; i < temperatures.size(); i++) {
            Double temperature = temperatures.get(i);
            Double consommation = consommations.get(i);

            if (temperature != null && consommation != null) {
                temperaturesValides.add(temperature);
                consommationsValides.add(consommation);
            }
        }

        if (temperaturesValides.size() < 2) {
            return 0.0;
        }

        double moyenneTemperature = temperaturesValides.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double moyenneConsommation = consommationsValides.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double num = 0, denTemp = 0, denConso = 0;
        for (int i = 0; i < temperaturesValides.size(); i++) {
            double dT = temperaturesValides.get(i) - moyenneTemperature;
            double dC = consommationsValides.get(i) - moyenneConsommation;
            num += dT * dC;
            denTemp += dT * dT;
            denConso += dC * dC;
        }

        if (denTemp == 0 || denConso == 0) {
            return 0.0;
        }

        return num / Math.sqrt(denTemp * denConso);
    }

    private Map<Integer, Double> calculerConsommationMoyenne(
            List<AnalysePoint> points,
            Function<AnalysePoint, Integer> groupe
    ) {
        return points.stream()
                .filter(point -> point.consommationMw() != null)
                .collect(Collectors.groupingBy(
                        groupe,
                        TreeMap::new,
                        Collectors.averagingDouble(AnalysePoint::consommationMw)
                ));
    }
}
