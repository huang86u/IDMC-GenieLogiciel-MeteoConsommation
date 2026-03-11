package com.example.genielogicielmeteoconsommation.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "consommation_electrique")
public class ConsommationElectrique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalTime heure;
    private String region;

    @Column(name = "consommation_mw")
    private Double consommationMw;

    // Constructeur vide obligatoire pour Spring
    public ConsommationElectrique() {}

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getHeure() { return heure; }
    public void setHeure(LocalTime heure) { this.heure = heure; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Double getConsommationMw() { return consommationMw; }
    public void setConsommationMw(Double consommationMw) { this.consommationMw = consommationMw; }
}