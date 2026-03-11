package com.example.genielogicielmeteoconsommation.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "donnees_meteo")
public class DonneesMeteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String departement; // Ex: "08", "54", etc.
    private String station;     // Le code NUM_POSTE de la station

    private LocalDate date;
    private LocalTime heure;

    private Double temperature;    // Colonne T
    private Double humidite;       // Colonne U
    private Double precipitations; // Colonne RR1
    private Double vent;           // Colonne FF

    // Constructeur vide obligatoire pour Spring Boot
    public DonneesMeteo() {}

    // --- GETTERS ET SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeure() { return heure; }
    public void setHeure(LocalTime heure) { this.heure = heure; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getHumidite() { return humidite; }
    public void setHumidite(Double humidite) { this.humidite = humidite; }

    public Double getPrecipitations() { return precipitations; }
    public void setPrecipitations(Double precipitations) { this.precipitations = precipitations; }

    public Double getVent() { return vent; }
    public void setVent(Double vent) { this.vent = vent; }
}