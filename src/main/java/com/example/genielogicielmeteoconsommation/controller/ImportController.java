package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.service.ImportDonneesService;
import com.example.genielogicielmeteoconsommation.service.ImportMeteoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/donnees")
public class ImportController {

    @Autowired
    private ImportDonneesService importService;

    @PostMapping("/importer-electricite")
    public ResponseEntity<String> importerElectricite(@RequestParam("file") MultipartFile file) {
        try {
            importService.importerFichierRte(file);
            return ResponseEntity.ok("Fichier RTE importé et filtré avec succès !");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
        }
    }

    @Autowired
    private ImportMeteoService importMeteoService;

    @PostMapping("/importer-meteo")
    public ResponseEntity<String> importerMeteoMultiple(@RequestParam("files") MultipartFile[] files) {
        try {
            for (MultipartFile file : files) {
                // On appelle le service pour chaque fichier reçu
                importMeteoService.importerFichierMeteo(file);
            }
            return ResponseEntity.ok(files.length + " fichier(s) Météo importé(s) avec succès !");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'import météo : " + e.getMessage());
        }
    }
}
