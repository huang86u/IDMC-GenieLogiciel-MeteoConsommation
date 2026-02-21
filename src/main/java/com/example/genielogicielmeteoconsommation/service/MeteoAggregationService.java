package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.entity.IndicateurQualite;
import com.example.genielogicielmeteoconsommation.entity.MeteoJournaliereDepartement;
import com.example.genielogicielmeteoconsommation.entity.ObservationMeteo;
import com.example.genielogicielmeteoconsommation.repository.MeteoJournaliereDepartementRepository;
import com.example.genielogicielmeteoconsommation.repository.ObservationMeteoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Service d'agrégation journalière des observations météo par département.
 * Passe des mesures horaires (ou demi-horaires) à un résumé journalier.
 */
@Service
public class MeteoAggregationService {

    private static final Logger log = LoggerFactory.getLogger(MeteoAggregationService.class);

    /**
     * Nombre minimum d'observations valides par jour pour considérer la qualité OK.
     * Une journée contient théoriquement 24 observations horaires ;
     * on exige au moins 18 pour valider la journée.
     */
    private static final int SEUIL_QUALITE_OK = 18;

    private static final int SCALE = 2;

    private final ObservationMeteoRepository observationRepo;
    private final MeteoJournaliereDepartementRepository meteoJournaliereRepo;

    public MeteoAggregationService(
            ObservationMeteoRepository observationRepo,
            MeteoJournaliereDepartementRepository meteoJournaliereRepo) {
        this.observationRepo = observationRepo;
        this.meteoJournaliereRepo = meteoJournaliereRepo;
    }

    /**
     * Agrège les observations météo horaires en résumés journaliers
     * pour un département et une période donnés.
     *
     * @param codeDepartement code du département (ex: "44", "67")
     * @param debut           date de début (incluse)
     * @param fin             date de fin (incluse)
     */
    @Transactional
    public void aggregerDepartement(String codeDepartement, LocalDate debut, LocalDate fin) {
        log.info("Début agrégation météo journalière — département : {}, du {} au {}",
                codeDepartement, debut, fin);

        // 1) Convertir LocalDate en LocalDateTime
        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt   = fin.atTime(23, 59, 59);

        // 2) Récupérer les observations triées par dateHeureLocale
        List<ObservationMeteo> observations =
                observationRepo.findByCodeDepartementAndDateHeureLocaleBetweenOrderByDateHeureLocaleAsc(
                        codeDepartement, debutDt, finDt);

        if (observations.isEmpty()) {
            log.warn("Aucune observation trouvée pour le département {} entre {} et {}. Agrégation annulée.",
                    codeDepartement, debut, fin);
            return;
        }

        log.info("{} observations récupérées pour le département {}.", observations.size(), codeDepartement);

        // 3) Supprimer les anciennes agrégations sur la période
        meteoJournaliereRepo.deleteByCodeDepartementAndDateBetween(codeDepartement, debut, fin);
        log.info("Anciennes agrégations supprimées pour le département {} entre {} et {}.",
                codeDepartement, debut, fin);

        // 4) Grouper par jour dans une TreeMap (ordre chronologique garanti)
        TreeMap<LocalDate, List<ObservationMeteo>> observationsParJour = observations.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDateHeureLocale().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        // 5) Calculer les agrégats journaliers
        List<MeteoJournaliereDepartement> agregations = new ArrayList<>();

        for (Map.Entry<LocalDate, List<ObservationMeteo>> entry : observationsParJour.entrySet()) {
            LocalDate jour = entry.getKey();
            List<ObservationMeteo> obsDuJour = entry.getValue();

            int nbTotal = obsDuJour.size();

            // --- Filtre indépendant par métrique (modification 1) ---

            // Température : référence principale pour nbValides (modification 2)
            List<ObservationMeteo> obsTemp = obsDuJour.stream()
                    .filter(o -> o.getTemperatureC() != null)
                    .collect(Collectors.toList());
            int nbValides = obsTemp.size(); // référence qualité (modification 2)

            if (nbValides == 0) {
                log.warn("Jour {} — aucune observation de température valide pour le département {} "
                        + "({} obs. brutes). Jour ignoré.", jour, codeDepartement, nbTotal);
                continue;
            }

            // Humidité
            List<ObservationMeteo> obsHumidite = obsDuJour.stream()
                    .filter(o -> o.getHumiditePct() != null)
                    .collect(Collectors.toList());

            // Vent
            List<ObservationMeteo> obsVent = obsDuJour.stream()
                    .filter(o -> o.getVitesseVentMs() != null)
                    .collect(Collectors.toList());

            // --- Calcul moyenne température ---
            BigDecimal sommeTemp = obsTemp.stream()
                    .map(o -> toBigDecimal(o.getTemperatureC()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal moyenneTemp = sommeTemp.divide(
                    BigDecimal.valueOf(obsTemp.size()), SCALE, RoundingMode.HALF_UP);

            // --- Calcul moyenne humidité (null si aucune obs valide) ---
            BigDecimal moyenneHumidite = null;
            if (!obsHumidite.isEmpty()) {
                BigDecimal sommeHumidite = obsHumidite.stream()
                        .map(o -> toBigDecimal(o.getHumiditePct()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                moyenneHumidite = sommeHumidite.divide(
                        BigDecimal.valueOf(obsHumidite.size()), SCALE, RoundingMode.HALF_UP);
            }

            // --- Calcul somme précipitations :
            //     null => traité comme 0 pour ne pas invalider le jour (modification 1) ---
            BigDecimal sommePrecipitations = obsDuJour.stream()
                    .map(o -> o.getPrecipitationsMm() != null
                            ? toBigDecimal(o.getPrecipitationsMm())
                            : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            // --- Calcul moyenne vitesse vent (null si aucune obs valide) ---
            BigDecimal moyenneVent = null;
            if (!obsVent.isEmpty()) {
                BigDecimal sommeVent = obsVent.stream()
                        .map(o -> toBigDecimal(o.getVitesseVentMs()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                moyenneVent = sommeVent.divide(
                        BigDecimal.valueOf(obsVent.size()), SCALE, RoundingMode.HALF_UP);
            }

            // --- Stations distinctes sur l'ensemble des obs du jour (modification 4) ---
            Set<String> stationsDistinctes = obsDuJour.stream()
                    .map(ObservationMeteo::getIdStation)
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toSet());
            int nbStations = stationsDistinctes.size();

            // --- Qualité basée sur nbValides = countTemp (modification 3) ---
            IndicateurQualite qualite = (nbValides >= SEUIL_QUALITE_OK)
                    ? IndicateurQualite.OK
                    : IndicateurQualite.INCOMPLET;

            // --- Construire l'agrégat ---
            MeteoJournaliereDepartement agregat = new MeteoJournaliereDepartement();
            agregat.setCodeDepartement(codeDepartement);
            agregat.setDate(jour);
            agregat.setTemperatureMoyenneC(moyenneTemp);
            agregat.setHumiditeMoyennePct(moyenneHumidite);
            agregat.setPrecipitationsTotalesMm(sommePrecipitations);
            agregat.setVitesseVentMoyenneMs(moyenneVent);
            agregat.setNbObservationsValides(nbValides);
            agregat.setNbStationsUtilisees(nbStations);
            agregat.setQualite(qualite);

            agregations.add(agregat);

            log.debug("Jour {} — {}/{} obs. temp. valides, {} stations — temp: {}°C, hum: {}%, "
                            + "précip: {}mm, vent: {}m/s — qualité: {}",
                    jour, nbValides, nbTotal, nbStations,
                    moyenneTemp, moyenneHumidite, sommePrecipitations, moyenneVent, qualite);
        }

        if (agregations.isEmpty()) {
            log.warn("Aucun agrégat à sauvegarder pour le département {} entre {} et {}.",
                    codeDepartement, debut, fin);
            return;
        }

        // 6) Sauvegarder toutes les agrégations
        meteoJournaliereRepo.saveAll(agregations);
        log.info("Agrégation météo terminée — {} entrées sauvegardées pour le département {}.",
                agregations.size(), codeDepartement);
    }

    // -------------------------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------------------------

    /**
     * Convertit un Number (Float, Double, BigDecimal...) en BigDecimal de manière sûre.
     */
    private BigDecimal toBigDecimal(Number value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return BigDecimal.valueOf(value.doubleValue());
    }
}