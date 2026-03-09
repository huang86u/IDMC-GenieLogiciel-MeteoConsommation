package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.entity.DonneesJournalieresFusionnees;
import com.example.genielogicielmeteoconsommation.resultat.ResultatAnalyseSaisonniere;
import com.example.genielogicielmeteoconsommation.resultat.ResultatCorrelation;
import com.example.genielogicielmeteoconsommation.resultat.StatistiqueTrancheTemperature;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyseMeteoConsommationService {

    private static final int ECHELLE = 4;

    // =========================================================================
    // 1. Corrélation de Pearson : température / consommation
    // =========================================================================

    public ResultatCorrelation calculerCorrelationTemperatureConsommation(
            List<DonneesJournalieresFusionnees> donnees) {

        List<DonneesJournalieresFusionnees> valides = filtrerTemperatureEtConsommation(donnees);

        if (valides.size() < 2) {
            return new ResultatCorrelation(
                    "temperatureMoyenne",
                    null,
                    valides.size(),
                    "Corrélation nulle ou non exploitable");
        }

        int n = valides.size();
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = valides.get(i).getTemperatureMoyenne().doubleValue();
            y[i] = valides.get(i).getConsommationMw().doubleValue();
        }

        double r = pearson(x, y);

        BigDecimal coefficient = BigDecimal.valueOf(r).setScale(ECHELLE, RoundingMode.HALF_UP);
        String interpretation = interpreter(r);

        return new ResultatCorrelation("temperatureMoyenne", coefficient, n, interpretation);
    }

    private double pearson(double[] x, double[] y) {
        int n = x.length;
        double moyX = moyenne(x);
        double moyY = moyenne(y);

        double numerateur = 0;
        double denomX = 0;
        double denomY = 0;

        for (int i = 0; i < n; i++) {
            double dx = x[i] - moyX;
            double dy = y[i] - moyY;
            numerateur += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }

        double denominateur = Math.sqrt(denomX * denomY);
        return denominateur == 0 ? 0 : numerateur / denominateur;
    }

    private double moyenne(double[] valeurs) {
        double somme = 0;
        for (double v : valeurs) somme += v;
        return somme / valeurs.length;
    }

    private String interpreter(double r) {
        if (r >= 0.7)  return "Forte corrélation positive";
        if (r >= 0.4)  return "Corrélation positive modérée";
        if (r >= 0.1)  return "Faible corrélation positive";
        if (r > -0.1)  return "Corrélation nulle ou non exploitable";
        if (r > -0.4)  return "Faible corrélation négative";
        if (r > -0.7)  return "Corrélation négative modérée";
        return "Forte corrélation négative";
    }

    // =========================================================================
    // 2. Statistiques par tranche de température
    // =========================================================================

    public List<StatistiqueTrancheTemperature> calculerStatistiquesParTrancheTemperature(
            List<DonneesJournalieresFusionnees> donnees) {

        List<DonneesJournalieresFusionnees> valides = filtrerTemperatureEtConsommation(donnees);

        Map<String, List<BigDecimal>> consommationsParTranche = new LinkedHashMap<>();
        for (String tranche : tranchesOrdonnees()) {
            consommationsParTranche.put(tranche, new ArrayList<>());
        }

        for (DonneesJournalieresFusionnees d : valides) {
            String tranche = determinerTranche(d.getTemperatureMoyenne().doubleValue());
            consommationsParTranche.get(tranche).add(d.getConsommationMw());
        }

        List<StatistiqueTrancheTemperature> resultats = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : consommationsParTranche.entrySet()) {
            List<BigDecimal> conso = entry.getValue();
            if (conso.isEmpty()) continue;

            BigDecimal somme = conso.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal moyenne = somme.divide(BigDecimal.valueOf(conso.size()), ECHELLE, RoundingMode.HALF_UP);

            resultats.add(new StatistiqueTrancheTemperature(entry.getKey(), moyenne, conso.size()));
        }

        return resultats;
    }

    private String determinerTranche(double temperature) {
        if (temperature < 0)   return "< 0°C";
        if (temperature < 5)   return "0 à 5°C";
        if (temperature < 10)  return "5 à 10°C";
        if (temperature < 15)  return "10 à 15°C";
        if (temperature < 20)  return "15 à 20°C";
        return ">= 20°C";
    }

    private List<String> tranchesOrdonnees() {
        return List.of("< 0°C", "0 à 5°C", "5 à 10°C", "10 à 15°C", "15 à 20°C", ">= 20°C");
    }

    // =========================================================================
    // 3. Analyse saisonnière
    // =========================================================================

    public List<ResultatAnalyseSaisonniere> calculerAnalyseSaisonniere(
            List<DonneesJournalieresFusionnees> donnees) {

        List<DonneesJournalieresFusionnees> valides = donnees.stream()
                .filter(d -> d.getDate() != null
                        && d.getConsommationMw() != null
                        && d.getTemperatureMoyenne() != null)
                .collect(Collectors.toList());

        Map<String, List<DonneesJournalieresFusionnees>> parSaison = new LinkedHashMap<>();
        for (String saison : saisonsOrdonnees()) {
            parSaison.put(saison, new ArrayList<>());
        }

        for (DonneesJournalieresFusionnees d : valides) {
            String saison = determinerSaison(d.getDate().getMonthValue());
            parSaison.get(saison).add(d);
        }

        List<ResultatAnalyseSaisonniere> resultats = new ArrayList<>();
        for (Map.Entry<String, List<DonneesJournalieresFusionnees>> entry : parSaison.entrySet()) {
            List<DonneesJournalieresFusionnees> groupe = entry.getValue();
            if (groupe.isEmpty()) continue;

            List<BigDecimal> consos = groupe.stream()
                    .map(DonneesJournalieresFusionnees::getConsommationMw)
                    .collect(Collectors.toList());

            List<BigDecimal> temperatures = groupe.stream()
                    .map(DonneesJournalieresFusionnees::getTemperatureMoyenne)
                    .collect(Collectors.toList());

            BigDecimal consommationMoyenne = moyenneBigDecimal(consos);
            BigDecimal temperatureMoyenne  = moyenneBigDecimal(temperatures);
            BigDecimal consommationMin     = consos.stream().min(BigDecimal::compareTo).orElse(null);
            BigDecimal consommationMax     = consos.stream().max(BigDecimal::compareTo).orElse(null);

            resultats.add(new ResultatAnalyseSaisonniere(
                    entry.getKey(),
                    consommationMoyenne,
                    temperatureMoyenne,
                    consommationMin,
                    consommationMax,
                    groupe.size()));
        }

        return resultats;
    }

    private String determinerSaison(int mois) {
        if (mois == 12 || mois <= 2) return "Hiver";
        if (mois <= 5)               return "Printemps";
        if (mois <= 8)               return "Été";
        return "Automne";
    }

    private List<String> saisonsOrdonnees() {
        return List.of("Hiver", "Printemps", "Été", "Automne");
    }

    // =========================================================================
    // Utilitaires privés
    // =========================================================================

    private List<DonneesJournalieresFusionnees> filtrerTemperatureEtConsommation(
            List<DonneesJournalieresFusionnees> donnees) {
        return donnees.stream()
                .filter(d -> d.getTemperatureMoyenne() != null && d.getConsommationMw() != null)
                .collect(Collectors.toList());
    }

    private BigDecimal moyenneBigDecimal(List<BigDecimal> valeurs) {
        BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return somme.divide(BigDecimal.valueOf(valeurs.size()), ECHELLE, RoundingMode.HALF_UP);
    }
}