package com.example.genielogicielmeteoconsommation.resultat;

import java.math.BigDecimal;

/**
 * Résultat d'une analyse saisonnière agrégant les indicateurs
 * de consommation électrique et de température pour une saison donnée.
 */
public class ResultatAnalyseSaisonniere {

    private String saison;

    private BigDecimal consommationMoyenne;

    private BigDecimal temperatureMoyenne;

    private BigDecimal consommationMin;

    private BigDecimal consommationMax;

    private Integer nombreJours;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public ResultatAnalyseSaisonniere() {
    }

    public ResultatAnalyseSaisonniere(
            String saison,
            BigDecimal consommationMoyenne,
            BigDecimal temperatureMoyenne,
            BigDecimal consommationMin,
            BigDecimal consommationMax,
            Integer nombreJours) {
        this.saison = saison;
        this.consommationMoyenne = consommationMoyenne;
        this.temperatureMoyenne = temperatureMoyenne;
        this.consommationMin = consommationMin;
        this.consommationMax = consommationMax;
        this.nombreJours = nombreJours;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getSaison() {
        return saison;
    }

    public void setSaison(String saison) {
        this.saison = saison;
    }

    public BigDecimal getConsommationMoyenne() {
        return consommationMoyenne;
    }

    public void setConsommationMoyenne(BigDecimal consommationMoyenne) {
        this.consommationMoyenne = consommationMoyenne;
    }

    public BigDecimal getTemperatureMoyenne() {
        return temperatureMoyenne;
    }

    public void setTemperatureMoyenne(BigDecimal temperatureMoyenne) {
        this.temperatureMoyenne = temperatureMoyenne;
    }

    public BigDecimal getConsommationMin() {
        return consommationMin;
    }

    public void setConsommationMin(BigDecimal consommationMin) {
        this.consommationMin = consommationMin;
    }

    public BigDecimal getConsommationMax() {
        return consommationMax;
    }

    public void setConsommationMax(BigDecimal consommationMax) {
        this.consommationMax = consommationMax;
    }

    public Integer getNombreJours() {
        return nombreJours;
    }

    public void setNombreJours(Integer nombreJours) {
        this.nombreJours = nombreJours;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ResultatAnalyseSaisonniere{" +
                "saison='" + saison + '\'' +
                ", consommationMoyenne=" + consommationMoyenne +
                ", temperatureMoyenne=" + temperatureMoyenne +
                ", consommationMin=" + consommationMin +
                ", consommationMax=" + consommationMax +
                ", nombreJours=" + nombreJours +
                '}';
    }
}