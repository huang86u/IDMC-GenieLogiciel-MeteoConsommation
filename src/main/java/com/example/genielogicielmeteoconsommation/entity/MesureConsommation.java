package com.example.genielogicielmeteoconsommation.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mesure brute de consommation électrique (souvent demi-horaire) par région.
 */
@Entity
@Table(
        name = "mesure_consommation",
        indexes = {
                @Index(name = "idx_mesure_conso_region_dateheure", columnList = "codeRegion,dateHeure")
        }
)
public class MesureConsommation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codeRegion;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    /**
     * Consommation en MW (mégawatts).
     * On stocke en BigDecimal pour éviter les erreurs de float/double.
     */
    @Column(precision = 12, scale = 4)
    private BigDecimal consommationMw;

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodeRegion() {
        return codeRegion;
    }

    public void setCodeRegion(String codeRegion) {
        this.codeRegion = codeRegion;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public BigDecimal getConsommationMw() {
        return consommationMw;
    }

    public void setConsommationMw(BigDecimal consommationMw) {
        this.consommationMw = consommationMw;
    }
}