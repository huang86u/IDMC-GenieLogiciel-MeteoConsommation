package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsommationElectriqueRepository extends JpaRepository<ConsommationElectrique, Long> {
    @Query("SELECT c.consommationMw FROM ConsommationElectrique c WHERE c.consommationMw IS NOT NULL ORDER BY c.date ASC, c.heure ASC")
    List<Double> findAllConsommations();
}
