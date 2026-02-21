package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.entity.ObservationMeteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ObservationMeteoRepository extends JpaRepository<ObservationMeteo, Integer> {

    /**
     * Récupère les observations météo d'un département sur une période,
     * triées par dateHeureLocale ascendante.
     *
     * @param codeDepartement code du département (ex: "44", "67")
     * @param debut           date/heure de début (incluse)
     * @param fin             date/heure de fin (incluse)
     * @return liste des observations triées chronologiquement
     */
    List<ObservationMeteo> findByCodeDepartementAndDateHeureLocaleBetweenOrderByDateHeureLocaleAsc(
            String codeDepartement,
            LocalDateTime debut,
            LocalDateTime fin
    );

    /**
     * Compte les observations d'un département sur une période.
     * Utile pour estimer le volume avant un traitement lourd.
     *
     * @param codeDepartement code du département
     * @param debut           date/heure de début (incluse)
     * @param fin             date/heure de fin (incluse)
     * @return nombre d'observations correspondantes
     */
    long countByCodeDepartementAndDateHeureLocaleBetween(
            String codeDepartement,
            LocalDateTime debut,
            LocalDateTime fin
    );
}