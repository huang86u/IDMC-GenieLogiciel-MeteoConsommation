package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsommationElectriqueRepository extends JpaRepository<ConsommationElectrique, Long> {
}