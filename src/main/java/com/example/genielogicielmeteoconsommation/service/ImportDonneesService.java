package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportDonneesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDonneesService.class);
    private static final int TAILLE_LOT = 1000;

    private final ConsommationElectriqueRepository repository;

    public ImportDonneesService(ConsommationElectriqueRepository repository) {
        this.repository = repository;
    }

    public void importerFichierRte(MultipartFile file) {
        List<ConsommationElectrique> listeAEnregistrer = new ArrayList<>();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int nombreLignesEnregistrees = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            boolean premiereLigne = true;

            while ((ligne = br.readLine()) != null) {
                if (premiereLigne) {
                    premiereLigne = false;
                    continue;
                }

                String[] colonnes = ligne.split(";", -1);
                if (colonnes.length < 5) {
                    continue;
                }

                String region = colonnes[0].trim();

                if (region.equalsIgnoreCase("France") || region.equalsIgnoreCase("Grand Est") ||
                        region.equalsIgnoreCase("Alsace") || region.equalsIgnoreCase("Champagne-Ardenne") ||
                        region.equalsIgnoreCase("Lorraine")) {

                    try {
                        String consoStr = colonnes[4].trim();
                        if (consoStr.isEmpty() || consoStr.equals("ND") || consoStr.equals("-")) {
                            continue;
                        }

                        ConsommationElectrique conso = new ConsommationElectrique();
                        conso.setRegion(region);

                        LocalDate date = LocalDate.parse(colonnes[2].trim(), dateFormatter);

                        String heureStr = colonnes[3].trim();
                        if (heureStr.equals("24:00")) {
                            heureStr = "00:00";
                            date = date.plusDays(1);
                        }

                        conso.setDate(date);
                        conso.setHeure(LocalTime.parse(heureStr));

                        conso.setConsommationMw(Double.parseDouble(consoStr.replace(",", ".")));

                        listeAEnregistrer.add(conso);

                        if (listeAEnregistrer.size() >= TAILLE_LOT) {
                            repository.saveAll(listeAEnregistrer);
                            nombreLignesEnregistrees += listeAEnregistrer.size();
                            listeAEnregistrer.clear();
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Ligne RTE ignoree car invalide: {}", ligne, e);
                    }
                }
            }

            if (!listeAEnregistrer.isEmpty()) {
                repository.saveAll(listeAEnregistrer);
                nombreLignesEnregistrees += listeAEnregistrer.size();
            }

            LOGGER.info("Importation RTE terminee: {} lignes ajoutees", nombreLignesEnregistrees);

        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier CSV", e);
        }
    }
}
