package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.service.ConsommationAggregationService;
import com.example.genielogicielmeteoconsommation.service.MeteoAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller REST pour déclencher les agrégations de consommation électrique et météo.
 *
 * Exemples d'appel :
 *   POST /api/aggregation/consommation?codeRegion=44&debut=2014-01-01&fin=2014-12-31
 *   POST /api/aggregation/meteo?codeDepartement=44&debut=2014-01-01&fin=2014-12-31
 */
@RestController
@RequestMapping("/api/aggregation")
public class AggregationController {

    private static final Logger log = LoggerFactory.getLogger(AggregationController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    private final ConsommationAggregationService consommationAggregationService;
    private final MeteoAggregationService meteoAggregationService;

    public AggregationController(
            ConsommationAggregationService consommationAggregationService,
            MeteoAggregationService meteoAggregationService) {
        this.consommationAggregationService = consommationAggregationService;
        this.meteoAggregationService = meteoAggregationService;
    }

    // =========================================================================
    // Endpoint consommation
    // =========================================================================

    /**
     * Déclenche l'agrégation journalière de consommation pour une région et une période.
     *
     * @param codeRegion code de la région (ex: "44", "GRAND_EST")
     * @param debut      date de début au format yyyy-MM-dd
     * @param fin        date de fin au format yyyy-MM-dd
     * @return réponse JSON avec statut de l'opération
     */
    @PostMapping("/consommation")
    public ResponseEntity<Map<String, String>> aggregerConsommation(
            @RequestParam String codeRegion,
            @RequestParam String debut,
            @RequestParam String fin) {

        // --- Validation : codeRegion non vide ---
        if (codeRegion == null || codeRegion.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Le paramètre 'codeRegion' est obligatoire et ne peut pas être vide."));
        }

        // --- Validation et parsing des dates ---
        LocalDate dateDebut;
        LocalDate dateFin;

        try {
            dateDebut = LocalDate.parse(debut, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Format de date invalide pour 'debut' : '" + debut
                            + "'. Format attendu : yyyy-MM-dd (ex: 2014-01-01)."));
        }

        try {
            dateFin = LocalDate.parse(fin, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Format de date invalide pour 'fin' : '" + fin
                            + "'. Format attendu : yyyy-MM-dd (ex: 2014-12-31)."));
        }

        if (dateDebut.isAfter(dateFin)) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("La date de début (" + debut
                            + ") doit être antérieure ou égale à la date de fin (" + fin + ")."));
        }

        // --- Déclenchement de l'agrégation ---
        log.info("Déclenchement agrégation consommation via API — région : {}, du {} au {}",
                codeRegion, dateDebut, dateFin);

        consommationAggregationService.aggregerRegion(codeRegion, dateDebut, dateFin);

        // --- Réponse succès ---
        Map<String, String> reponse = new LinkedHashMap<>();
        reponse.put("message", "Agrégation journalière effectuée avec succès.");
        reponse.put("codeRegion", codeRegion);
        reponse.put("debut", dateDebut.toString());
        reponse.put("fin", dateFin.toString());

        return ResponseEntity.ok(reponse);
    }

    // =========================================================================
    // Endpoint météo
    // =========================================================================

    /**
     * Déclenche l'agrégation journalière météo pour un département et une période.
     *
     * @param codeDepartement code du département (ex: "44", "67")
     * @param debut           date de début au format yyyy-MM-dd
     * @param fin             date de fin au format yyyy-MM-dd
     * @return réponse JSON avec statut de l'opération
     */
    @PostMapping("/meteo")
    public ResponseEntity<Map<String, String>> aggregerMeteo(
            @RequestParam String codeDepartement,
            @RequestParam String debut,
            @RequestParam String fin) {

        // --- Validation : codeDepartement non vide ---
        if (codeDepartement == null || codeDepartement.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Le paramètre 'codeDepartement' est obligatoire et ne peut pas être vide."));
        }

        // --- Validation et parsing des dates ---
        LocalDate dateDebut;
        LocalDate dateFin;

        try {
            dateDebut = LocalDate.parse(debut, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Format de date invalide pour 'debut' : '" + debut
                            + "'. Format attendu : yyyy-MM-dd (ex: 2014-01-01)."));
        }

        try {
            dateFin = LocalDate.parse(fin, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("Format de date invalide pour 'fin' : '" + fin
                            + "'. Format attendu : yyyy-MM-dd (ex: 2014-12-31)."));
        }

        if (dateDebut.isAfter(dateFin)) {
            return ResponseEntity
                    .badRequest()
                    .body(erreur("La date de début (" + debut
                            + ") doit être antérieure ou égale à la date de fin (" + fin + ")."));
        }

        // --- Déclenchement de l'agrégation ---
        log.info("Déclenchement agrégation météo via API — département : {}, du {} au {}",
                codeDepartement, dateDebut, dateFin);

        meteoAggregationService.aggregerDepartement(codeDepartement, dateDebut, dateFin);

        // --- Réponse succès ---
        Map<String, String> reponse = new LinkedHashMap<>();
        reponse.put("message", "Agrégation météo effectuée avec succès.");
        reponse.put("codeDepartement", codeDepartement);
        reponse.put("debut", dateDebut.toString());
        reponse.put("fin", dateFin.toString());

        return ResponseEntity.ok(reponse);
    }

    // =========================================================================
    // Utilitaire
    // =========================================================================

    /**
     * Construit une map d'erreur JSON standardisée.
     */
    private Map<String, String> erreur(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("erreur", message);
        return body;
    }
}