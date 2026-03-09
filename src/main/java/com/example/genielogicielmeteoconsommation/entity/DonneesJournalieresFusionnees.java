package com.example.genielogicielmeteoconsommation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fusions entre météo et consommation électrique.
 */
public class DonneesJournalieresFusionnees {

    private LocalDate date;

    private String codeRegion;

    private BigDecimal temperatureMoyenne;

    private BigDecimal humiditeMoyenne;

    private BigDecimal precipitationTotale;

    private BigDecimal ventMoyen;

    private BigDecimal consommationMw;

    private IndicateurQualite qualite;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public DonneesJournalieresFusionnees() {
    }

    public DonneesJournalieresFusionnees(
            LocalDate date,
            String codeRegion,
            BigDecimal temperatureMoyenne,
            BigDecimal humiditeMoyenne,
            BigDecimal precipitationTotale,
            BigDecimal ventMoyen,
            BigDecimal consommationMw,
            IndicateurQualite qualite) {
        this.date = date;
        this.codeRegion = codeRegion;
        this.temperatureMoyenne = temperatureMoyenne;
        this.humiditeMoyenne = humiditeMoyenne;
        this.precipitationTotale = precipitationTotale;
        this.ventMoyen = ventMoyen;
        this.consommationMw = consommationMw;
        this.qualite = qualite;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCodeRegion() {
        return codeRegion;
    }

    public void setCodeRegion(String codeRegion) {
        this.codeRegion = codeRegion;
    }

    public BigDecimal getTemperatureMoyenne() {
        return temperatureMoyenne;
    }

    public void setTemperatureMoyenne(BigDecimal temperatureMoyenne) {
        this.temperatureMoyenne = temperatureMoyenne;
    }

    public BigDecimal getHumiditeMoyenne() {
        return humiditeMoyenne;
    }

    public void setHumiditeMoyenne(BigDecimal humiditeMoyenne) {
        this.humiditeMoyenne = humiditeMoyenne;
    }

    public BigDecimal getPrecipitationTotale() {
        return precipitationTotale;
    }

    public void setPrecipitationTotale(BigDecimal precipitationTotale) {
        this.precipitationTotale = precipitationTotale;
    }

    public BigDecimal getVentMoyen() {
        return ventMoyen;
    }

    public void setVentMoyen(BigDecimal ventMoyen) {
        this.ventMoyen = ventMoyen;
    }

    public BigDecimal getConsommationMw() {
        return consommationMw;
    }

    public void setConsommationMw(BigDecimal consommationMw) {
        this.consommationMw = consommationMw;
    }

    public IndicateurQualite getQualite() {
        return qualite;
    }

    public void setQualite(IndicateurQualite qualite) {
        this.qualite = qualite;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "DonneesJournalieresFusionnees{" +
                "date=" + date +
                ", codeRegion='" + codeRegion + '\'' +
                ", temperatureMoyenne=" + temperatureMoyenne +
                ", humiditeMoyenne=" + humiditeMoyenne +
                ", precipitationTotale=" + precipitationTotale +
                ", ventMoyen=" + ventMoyen +
                ", consommationMw=" + consommationMw +
                ", qualite=" + qualite +
                '}';
    }
}