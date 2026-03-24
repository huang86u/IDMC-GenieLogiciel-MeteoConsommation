package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsommationElectriqueRepository extends JpaRepository<ConsommationElectrique, Long> {

    List<ConsommationElectrique> findAllByDateBetween(LocalDate startDate, LocalDate endDate);
}
