package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.entity.ConsommationJournaliereRegion;
import com.example.genielogicielmeteoconsommation.entity.IndicateurQualite;
import com.example.genielogicielmeteoconsommation.entity.MesureConsommation;
import com.example.genielogicielmeteoconsommation.repository.ConsommationJournaliereRegionRepository;
import com.example.genielogicielmeteoconsommation.repository.MesureConsommationRepository;
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
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ConsommationAggregationService {

    private static final Logger log = LoggerFactory.getLogger(ConsommationAggregationService.class);

    /**
     * Nombre minimal de mesures valides pour qu'un agrégat soit considéré OK.
     * Une journée contient théoriquement 48 mesures demi-horaires ;
     * on tolère un manque jusqu'à ce seuil.
     */
    private static final int SEUIL_QUALITE_OK = 40;

    private static final int SCALE = 4;

    private final MesureConsommationRepository mesureRepo;
    private final ConsommationJournaliereRegionRepository journaliereRepo;

    public ConsommationAggregationService(
            MesureConsommationRepository mesureRepo,
            ConsommationJournaliereRegionRepository journaliereRepo) {
        this.mesureRepo = mesureRepo;
        this.journaliereRepo = journaliereRepo;
    }

    @Transactional
    public void aggregerRegion(String codeRegion, LocalDate debut, LocalDate fin) {
        log.info("Début agrégation journalière — région : {}, du {} au {}", codeRegion, debut, fin);

        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt = fin.atTime(23, 59, 59);

        List<MesureConsommation> mesures =
                mesureRepo.findByCodeRegionAndDateHeureBetweenOrderByDateHeureAsc(codeRegion, debutDt, finDt);

        if (mesures.isEmpty()) {
            log.warn("Aucune mesure trouvée pour la région {} entre {} et {}. Agrégation annulée.",
                    codeRegion, debut, fin);
            return;
        }

        log.info("{} mesures récupérées pour la région {}.", mesures.size(), codeRegion);

        journaliereRepo.deleteByCodeRegionAndDateBetween(codeRegion, debut, fin);
        log.info("Anciennes agrégations supprimées pour la région {} entre {} et {}.",
                codeRegion, debut, fin);

        TreeMap<LocalDate, List<MesureConsommation>> mesuresParJour = mesures.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getDateHeure().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<ConsommationJournaliereRegion> agregations = new ArrayList<>();

        for (var entry : mesuresParJour.entrySet()) {
            LocalDate jour = entry.getKey();
            List<MesureConsommation> mesuresDuJour = entry.getValue();

            // ⚠️ IMPORTANT : ici on suppose que getConsommationMw() renvoie BigDecimal
            // Si ton entity renvoie Float/Double, dis-moi et j'adapte.
            List<MesureConsommation> mesuresValides = mesuresDuJour.stream()
                    .filter(m -> m.getConsommationMw() != null
                            && m.getConsommationMw().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());

            int nbValides = mesuresValides.size();

            if (nbValides == 0) {
                log.warn("Jour {} — aucune mesure valide pour la région {}. Jour ignoré.", jour, codeRegion);
                continue;
            }

            IndicateurQualite qualite = (nbValides >= SEUIL_QUALITE_OK)
                    ? IndicateurQualite.OK
                    : IndicateurQualite.INCOMPLET;

            BigDecimal somme = mesuresValides.stream()
                    .map(MesureConsommation::getConsommationMw)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal moyenne = somme.divide(
                    BigDecimal.valueOf(nbValides),
                    SCALE,
                    RoundingMode.HALF_UP
            );

            ConsommationJournaliereRegion agregat = new ConsommationJournaliereRegion();
            agregat.setCodeRegion(codeRegion);
            agregat.setDate(jour);
            agregat.setConsommationMw(moyenne); // ✅ BigDecimal, pas float
            agregat.setNbMesuresValides(nbValides);
            agregat.setQualite(qualite);

            agregations.add(agregat);

            log.debug("Jour {} — {}/{} mesures valides — moyenne : {} MW — qualité : {}",
                    jour, nbValides, mesuresDuJour.size(), moyenne, qualite);
        }

        if (agregations.isEmpty()) {
            log.warn("Aucun agrégat à sauvegarder pour la région {} entre {} et {}.",
                    codeRegion, debut, fin);
            return;
        }

        journaliereRepo.saveAll(agregations);
        log.info("Agrégation terminée — {} entrées sauvegardées pour la région {}.",
                agregations.size(), codeRegion);
    }
}