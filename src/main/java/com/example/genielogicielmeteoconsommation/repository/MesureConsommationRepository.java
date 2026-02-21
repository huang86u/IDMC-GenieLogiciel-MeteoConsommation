package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.entity.MesureConsommation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MesureConsommationRepository extends JpaRepository<MesureConsommation, Integer> {

    /**
     * Récupère les mesures d'une région sur une période donnée.
     *
     * @param codeRegion le code de la région
     * @param debut      date/heure de début (incluse)
     * @param fin        date/heure de fin (incluse)
     * @return liste des mesures correspondantes
     */
    List<MesureConsommation> findByCodeRegionAndDateHeureBetween(
            String codeRegion,
            LocalDateTime debut,
            LocalDateTime fin);

    /**
     * Récupère les mesures d'une région sur une période donnée,
     * triées par dateHeure ascendante.
     *
     * @param codeRegion le code de la région
     * @param debut      date/heure de début (incluse)
     * @param fin        date/heure de fin (incluse)
     * @return liste des mesures triées par ordre chronologique
     */
    List<MesureConsommation> findByCodeRegionAndDateHeureBetweenOrderByDateHeureAsc(
            String codeRegion,
            LocalDateTime debut,
            LocalDateTime fin);
}