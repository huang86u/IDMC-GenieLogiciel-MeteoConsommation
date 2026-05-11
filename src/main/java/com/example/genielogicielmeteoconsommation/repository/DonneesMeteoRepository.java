package com.example.genielogicielmeteoconsommation.repository;

import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.service.AnalysePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonneesMeteoRepository extends JpaRepository<DonneesMeteo, Long> {

    @Query("SELECT m.temperature FROM DonneesMeteo m WHERE m.temperature IS NOT NULL ORDER BY m.date ASC, m.heure ASC")
    List<Double> findAllTemperatures();

    @Query("""
            SELECT new com.example.genielogicielmeteoconsommation.service.AnalysePoint(
                m.date,
                m.heure,
                AVG(m.temperature),
                AVG(m.humidite),
                AVG(m.precipitations),
                AVG(m.vent),
                AVG(c.consommationMw)
            )
            FROM DonneesMeteo m, ConsommationElectrique c
            WHERE m.date = c.date
              AND m.heure = c.heure
              AND c.consommationMw IS NOT NULL
              AND LOWER(c.region) IN ('grand est', 'alsace', 'champagne-ardenne', 'lorraine')
            GROUP BY m.date, m.heure
            ORDER BY m.date ASC, m.heure ASC
            """)
    List<AnalysePoint> findAnalysePoints();
}
