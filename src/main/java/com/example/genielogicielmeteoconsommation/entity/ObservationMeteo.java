package com.example.genielogicielmeteoconsommation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Observation météo brute (souvent horaire) associée à une station et un département.
 */
@Entity
@Table(
        name = "observation_meteo",
        indexes = {
                @Index(name = "idx_obs_meteo_dept_dateheure", columnList = "codeDepartement,dateHeureLocale"),
                @Index(name = "idx_obs_meteo_station_dateheure", columnList = "idStation,dateHeureLocale")
        }
)
public class ObservationMeteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codeDepartement;

    @Column(nullable = false, length = 20)
    private String idStation;

    /**
     * Date/heure locale (Paris) pour l'analyse/agrégation journalière.
     */
    @Column(nullable = false)
    private LocalDateTime dateHeureLocale;

    @Column(precision = 6, scale = 2)
    private BigDecimal temperatureC;

    @Column(precision = 6, scale = 2)
    private BigDecimal humiditePct;

    /**
     * Précipitations sur l'intervalle (mm). Peut être null => traité comme 0 par ton service.
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal precipitationsMm;

    @Column(precision = 6, scale = 2)
    private BigDecimal vitesseVentMs;

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

    public String getIdStation() {
        return idStation;
    }

    public void setIdStation(String idStation) {
        this.idStation = idStation;
    }

    public LocalDateTime getDateHeureLocale() {
        return dateHeureLocale;
    }

    public void setDateHeureLocale(LocalDateTime dateHeureLocale) {
        this.dateHeureLocale = dateHeureLocale;
    }

    public BigDecimal getTemperatureC() {
        return temperatureC;
    }

    public void setTemperatureC(BigDecimal temperatureC) {
        this.temperatureC = temperatureC;
    }

    public BigDecimal getHumiditePct() {
        return humiditePct;
    }

    public void setHumiditePct(BigDecimal humiditePct) {
        this.humiditePct = humiditePct;
    }

    public BigDecimal getPrecipitationsMm() {
        return precipitationsMm;
    }

    public void setPrecipitationsMm(BigDecimal precipitationsMm) {
        this.precipitationsMm = precipitationsMm;
    }

    public BigDecimal getVitesseVentMs() {
        return vitesseVentMs;
    }

    public void setVitesseVentMs(BigDecimal vitesseVentMs) {
        this.vitesseVentMs = vitesseVentMs;
    }
}