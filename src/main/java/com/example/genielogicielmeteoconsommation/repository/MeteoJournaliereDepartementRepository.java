package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.entity.MeteoJournaliereDepartement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MeteoJournaliereDepartementRepository extends JpaRepository<MeteoJournaliereDepartement, Integer> {

    /**
     * Supprime les agrégats météo journaliers d'un département sur une période donnée.
     *
     * @param codeDepartement code du département (ex: "44", "67")
     * @param debut           date de début (incluse)
     * @param fin             date de fin (incluse)
     */
    @Transactional
    void deleteByCodeDepartementAndDateBetween(
            String codeDepartement,
            LocalDate debut,
            LocalDate fin
    );

    /**
     * Récupère les agrégats météo journaliers d'un département sur une période donnée.
     *
     * @param codeDepartement code du département
     * @param debut           date de début (incluse)
     * @param fin             date de fin (incluse)
     * @return liste des agrégats météo correspondants
     */
    List<MeteoJournaliereDepartement> findByCodeDepartementAndDateBetween(
            String codeDepartement,
            LocalDate debut,
            LocalDate fin
    );
}