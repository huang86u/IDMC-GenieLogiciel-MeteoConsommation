package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.dto.ImportSummary;
import com.example.genielogicielmeteoconsommation.service.DashboardService;
import com.example.genielogicielmeteoconsommation.service.ImportDonneesService;
import com.example.genielogicielmeteoconsommation.service.ImportMeteoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/donnees")
public class ImportController {

    private final ImportDonneesService importService;
    private final ImportMeteoService importMeteoService;
    private final DashboardService dashboardService;

    public ImportController(
            ImportDonneesService importService,
            ImportMeteoService importMeteoService,
            DashboardService dashboardService
    ) {
        this.importService = importService;
        this.importMeteoService = importMeteoService;
        this.dashboardService = dashboardService;
    }

    @PostMapping("/importer-electricite")
    public ResponseEntity<String> importerElectricite(@RequestParam("file") MultipartFile file) {
        try {
            ImportSummary summary = importService.importerFichierRte(file);
            dashboardService.clearOverviewCache();
            String message = "Import electricite termine : %d lignes valides ajoutees, %d lignes ignorees."
                    .formatted(summary.insertedRows(), summary.skippedRows());
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (Exception exception) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'import electricite : " + exception.getMessage());
        }
    }

    @PostMapping("/importer-meteo")
    public ResponseEntity<String> importerMeteoMultiple(@RequestParam("files") MultipartFile[] files) {
        try {
            int insertedRows = 0;
            int skippedRows = 0;

            for (MultipartFile file : files) {
                ImportSummary summary = importMeteoService.importerFichierMeteo(file);
                insertedRows += summary.insertedRows();
                skippedRows += summary.skippedRows();
            }

            dashboardService.clearOverviewCache();
            String message = "Import meteo termine : %d fichiers traites, %d lignes valides ajoutees, %d lignes ignorees."
                    .formatted(files.length, insertedRows, skippedRows);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (Exception exception) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de l'import meteo : " + exception.getMessage());
        }
    }
}
