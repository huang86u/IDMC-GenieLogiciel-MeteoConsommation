package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportDonneesService {

    @Autowired
    private ConsommationElectriqueRepository repository;

    public void importerFichierRte(MultipartFile file) {
        List<ConsommationElectrique> listeAEnregistrer = new ArrayList<>();

        // CORRECTION 1 : Le bon format de date pour votre fichier
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String ligne;
            boolean premiereLigne = true;

            while ((ligne = br.readLine()) != null) {
                if (premiereLigne) {
                    premiereLigne = false;
                    continue;
                }

                String[] colonnes = ligne.split(";");
                if(colonnes.length < 5) continue;

                String region = colonnes[0].trim();

                // CORRECTION 2 : J'ajoute "France" temporairement pour que vous puissiez tester votre fichier
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

                        // On utilise le nouveau formateur de date
                        conso.setDate(LocalDate.parse(colonnes[2].trim(), dateFormatter));

                        String heureStr = colonnes[3].trim();
                        if(heureStr.equals("24:00")) heureStr = "00:00";
                        conso.setHeure(LocalTime.parse(heureStr));

                        conso.setConsommationMw(Double.parseDouble(consoStr.replace(",", ".")));

                        listeAEnregistrer.add(conso);
                    } catch (Exception e) {
                        // Pour le test, on peut afficher l'erreur pour comprendre ce qui bloque
                        System.out.println("Erreur sur la ligne : " + ligne);
                    }
                }
            }

            repository.saveAll(listeAEnregistrer);
            System.out.println("Importation terminée : " + listeAEnregistrer.size() + " lignes ajoutées !");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la lecture du fichier CSV");
        }
    }
}