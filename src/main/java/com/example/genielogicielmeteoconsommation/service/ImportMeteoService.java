package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportMeteoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportMeteoService.class);
    private static final int TAILLE_LOT = 1000;

    private final DonneesMeteoRepository meteoRepository;

    public ImportMeteoService(DonneesMeteoRepository meteoRepository) {
        this.meteoRepository = meteoRepository;
    }

    public void importerFichierMeteo(MultipartFile file) {
        List<DonneesMeteo> listeAEnregistrer = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        ZoneId zoneUtc = ZoneId.of("UTC");
        ZoneId zoneParis = ZoneId.of("Europe/Paris");
        int nombreLignesEnregistrees = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String ligne;
            boolean premiereLigne = true;

            while ((ligne = br.readLine()) != null) {
                if (premiereLigne) {
                    premiereLigne = false;
                    continue; // On passe la ligne d'en-tête
                }

                String[] colonnes = ligne.split(";", -1);

                // Si la ligne est trop courte, on l'ignore
                if (colonnes.length < 77) {
                    continue;
                }

                try {
                    String numPoste = colonnes[0].trim();
                    String codeDepartement = numPoste.length() >= 2 ? numPoste.substring(0, 2) : "XX";
                    String dateHeureStr = colonnes[5].trim();

                    // Filtrage strict sur l'année 2014
                    if (!dateHeureStr.startsWith("2014")) {
                        continue;
                    }

                    // --- Gestion du Temps (UTC vers Europe/Paris) ---
                    LocalDateTime ldt = LocalDateTime.parse(dateHeureStr, formatter);
                    ZonedDateTime utcTime = ldt.atZone(zoneUtc);
                    ZonedDateTime parisTime = utcTime.withZoneSameInstant(zoneParis);

                    DonneesMeteo meteo = new DonneesMeteo();
                    meteo.setDepartement(codeDepartement);
                    meteo.setStation(numPoste);
                    meteo.setDate(parisTime.toLocalDate());
                    meteo.setHeure(parisTime.toLocalTime());

                    // Extraction des variables avec les bons indices du fichier CSV
                    meteo.setPrecipitations(parseValeur(colonnes[6])); // RR1
                    meteo.setVent(parseValeur(colonnes[10]));          // FF
                    meteo.setTemperature(parseValeur(colonnes[42]));         // T
                    meteo.setHumidite(parseValeur(colonnes[76]));          // U

                    listeAEnregistrer.add(meteo);

                    if (listeAEnregistrer.size() >= TAILLE_LOT) {
                        meteoRepository.saveAll(listeAEnregistrer);
                        nombreLignesEnregistrees += listeAEnregistrer.size();
                        listeAEnregistrer.clear();
                    }
                } catch (Exception e) {
                    LOGGER.warn("Ligne meteo ignoree car invalide: {}", ligne, e);
                }
            }

            if (!listeAEnregistrer.isEmpty()) {
                meteoRepository.saveAll(listeAEnregistrer);
                nombreLignesEnregistrees += listeAEnregistrer.size();
            }

            LOGGER.info(
                    "Importation meteo terminee pour le fichier {}: {} lignes ajoutees",
                    file.getOriginalFilename(),
                    nombreLignesEnregistrees
            );

        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la lecture du fichier meteo", e);
        }
    }

    // Méthode utilitaire pour gérer les valeurs vides ou mal formatées
    private Double parseValeur(String valeur) {
        if (valeur == null || valeur.trim().isEmpty() || valeur.equalsIgnoreCase("ND")) {
            return null;
        }
        try {
            return Double.parseDouble(valeur.replace(",", "."));
        } catch(NumberFormatException e) {
            return null;
        }
    }
}
