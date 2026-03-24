package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface DonneesMeteoRepository extends JpaRepository<DonneesMeteo, Long> {

    List<DonneesMeteo> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

    List<DonneesMeteo> findAllByDateBetweenAndDepartementIn(
            LocalDate startDate,
            LocalDate endDate,
            Collection<String> departements
    );
}
