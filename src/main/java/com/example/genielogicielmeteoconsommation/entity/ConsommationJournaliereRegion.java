package com.example.genielogicielmeteoconsommation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "consommation_journaliere_region",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"codeRegion", "date"})
        }
)
public class ConsommationJournaliereRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codeRegion;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal consommationMw;

    @Column(nullable = false)
    private int nbMesuresValides;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndicateurQualite qualite;

    // Getters & Setters

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodeRegion() { return codeRegion; }
    public void setCodeRegion(String codeRegion) { this.codeRegion = codeRegion; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getConsommationMw() { return consommationMw; }
    public void setConsommationMw(BigDecimal consommationMw) { this.consommationMw = consommationMw; }

    public int getNbMesuresValides() { return nbMesuresValides; }
    public void setNbMesuresValides(int nbMesuresValides) { this.nbMesuresValides = nbMesuresValides; }

    public IndicateurQualite getQualite() { return qualite; }
    public void setQualite(IndicateurQualite qualite) { this.qualite = qualite; }
}