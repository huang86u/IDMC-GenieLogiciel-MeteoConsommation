package com.example.genielogicielmeteoconsommation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agrégat journalier météo par département.
 */
@Entity
@Table(
        name = "meteo_journaliere_departement",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"codeDepartement", "date"})
        }
)
public class MeteoJournaliereDepartement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codeDepartement;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 6, scale = 2)
    private BigDecimal temperatureMoyenneC;

    @Column(precision = 6, scale = 2)
    private BigDecimal humiditeMoyennePct;

    @Column(precision = 8, scale = 2)
    private BigDecimal precipitationsTotalesMm;

    @Column(precision = 6, scale = 2)
    private BigDecimal vitesseVentMoyenneMs;

    @Column(nullable = false)
    private int nbObservationsValides;

    @Column(nullable = false)
    private int nbStationsUtilisees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndicateurQualite qualite;

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodeDepartement() {
        return codeDepartement;
    }

    public void setCodeDepartement(String codeDepartement) {
        this.codeDepartement = codeDepartement;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTemperatureMoyenneC() {
        return temperatureMoyenneC;
    }

    public void setTemperatureMoyenneC(BigDecimal temperatureMoyenneC) {
        this.temperatureMoyenneC = temperatureMoyenneC;
    }

    public BigDecimal getHumiditeMoyennePct() {
        return humiditeMoyennePct;
    }

    public void setHumiditeMoyennePct(BigDecimal humiditeMoyennePct) {
        this.humiditeMoyennePct = humiditeMoyennePct;
    }

    public BigDecimal getPrecipitationsTotalesMm() {
        return precipitationsTotalesMm;
    }

    public void setPrecipitationsTotalesMm(BigDecimal precipitationsTotalesMm) {
        this.precipitationsTotalesMm = precipitationsTotalesMm;
    }

    public BigDecimal getVitesseVentMoyenneMs() {
        return vitesseVentMoyenneMs;
    }

    public void setVitesseVentMoyenneMs(BigDecimal vitesseVentMoyenneMs) {
        this.vitesseVentMoyenneMs = vitesseVentMoyenneMs;
    }

    public int getNbObservationsValides() {
        return nbObservationsValides;
    }

    public void setNbObservationsValides(int nbObservationsValides) {
        this.nbObservationsValides = nbObservationsValides;
    }

    public int getNbStationsUtilisees() {
        return nbStationsUtilisees;
    }

    public void setNbStationsUtilisees(int nbStationsUtilisees) {
        this.nbStationsUtilisees = nbStationsUtilisees;
    }

    public IndicateurQualite getQualite() {
        return qualite;
    }

    public void setQualite(IndicateurQualite qualite) {
        this.qualite = qualite;
    }
}