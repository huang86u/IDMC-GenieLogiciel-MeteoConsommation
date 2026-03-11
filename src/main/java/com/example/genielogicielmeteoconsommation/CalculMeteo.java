package com.example.genielogicielmeteoconsommation;

import java.util.List;

public class CalculMeteo {

    /**
     * Calcule la moyenne pour synchroniser les données de 2014[cite: 34, 61].
     * Utile pour la température (T), l'humidité (U) ou le vent (FF)[cite: 42, 43, 45].
     */
    public double calculerMoyenneMeteo(List<Double> donneesHoraires) {
        if (donneesHoraires == null || donneesHoraires.isEmpty()) return 0.0;
        double somme = 0;
        for (Double valeur : donneesHoraires) {
            somme += valeur;
        }
        return somme / donneesHoraires.size();
    }

    /**
     * ALGO DE CORRÉLATION : Pour voir si la consommation suit la météo[cite: 65].
     * Compare par exemple la Température (T) et la Consommation (MW)[cite: 51, 65].
     */
    public double calculerCorrelation(List<Double> meteo, List<Double> consommation) {
        if (meteo == null || consommation == null || meteo.size() != consommation.size() || meteo.isEmpty()) {
            return 0.0;
        }

        double moyenneMeteo = calculerMoyenneMeteo(meteo);
        double moyenneConso = calculerMoyenneMeteo(consommation);

        double numerateur = 0;
        double denometeurMeteo = 0;
        double denominateurConso = 0;

        for (int i = 0; i < meteo.size(); i++) {
            double diffMeteo = meteo.get(i) - moyenneMeteo;
            double diffConso = consommation.get(i) - moyenneConso;
            
            numerateur += diffMeteo * diffConso;
            denometeurMeteo += diffMeteo * diffMeteo;
            denominateurConso += diffConso * diffConso;
        }

        return numerateur / Math.sqrt(denometeurMeteo * denominateurConso);
    }
}