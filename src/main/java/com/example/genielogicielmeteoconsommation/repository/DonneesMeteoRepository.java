package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonneesMeteoRepository extends JpaRepository<DonneesMeteo, Long> {
}