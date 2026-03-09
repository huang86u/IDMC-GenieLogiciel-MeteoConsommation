package com.example.genielogicielmeteoconsommation.resultat;

import java.math.BigDecimal;

public class ResultatCorrelation {

    private String variableMeteo;

    private BigDecimal coefficientCorrelation;

    private Integer nombreObservations;

    private String interpretation;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public ResultatCorrelation() {
    }

    public ResultatCorrelation(
            String variableMeteo,
            BigDecimal coefficientCorrelation,
            Integer nombreObservations,
            String interpretation) {
        this.variableMeteo = variableMeteo;
        this.coefficientCorrelation = coefficientCorrelation;
        this.nombreObservations = nombreObservations;
        this.interpretation = interpretation;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getVariableMeteo() {
        return variableMeteo;
    }

    public void setVariableMeteo(String variableMeteo) {
        this.variableMeteo = variableMeteo;
    }

    public BigDecimal getCoefficientCorrelation() {
        return coefficientCorrelation;
    }

    public void setCoefficientCorrelation(BigDecimal coefficientCorrelation) {
        this.coefficientCorrelation = coefficientCorrelation;
    }

    public Integer getNombreObservations() {
        return nombreObservations;
    }

    public void setNombreObservations(Integer nombreObservations) {
        this.nombreObservations = nombreObservations;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ResultatCorrelation{" +
                "variableMeteo='" + variableMeteo + '\'' +
                ", coefficientCorrelation=" + coefficientCorrelation +
                ", nombreObservations=" + nombreObservations +
                ", interpretation='" + interpretation + '\'' +
                '}';
    }
}