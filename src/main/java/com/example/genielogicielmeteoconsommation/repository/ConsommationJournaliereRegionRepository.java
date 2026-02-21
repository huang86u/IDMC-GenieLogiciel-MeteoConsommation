package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.entity.ConsommationJournaliereRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsommationJournaliereRegionRepository extends JpaRepository<ConsommationJournaliereRegion, Integer> {

    /**
     * Supprime les agrégats journaliers d'une région sur une période donnée.
     *
     * @param codeRegion le code de la région
     * @param debut      date de début (incluse)
     * @param fin        date de fin (incluse)
     */
    @Transactional
    void deleteByCodeRegionAndDateBetween(
            String codeRegion,
            LocalDate debut,
            LocalDate fin
    );

    /**
     * Récupère les agrégats journaliers d'une région sur une période donnée.
     *
     * @param codeRegion le code de la région
     * @param debut      date de début (incluse)
     * @param fin        date de fin (incluse)
     * @return liste des consommations journalières correspondantes
     */
    List<ConsommationJournaliereRegion> findByCodeRegionAndDateBetween(
            String codeRegion,
            LocalDate debut,
            LocalDate fin
    );
}