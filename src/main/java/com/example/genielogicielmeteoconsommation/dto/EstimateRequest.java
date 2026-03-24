package com.example.genielogicielmeteoconsommation.dto;

import java.time.LocalDate;
import java.util.List;

public record EstimateRequest(
        List<String> departments,
        LocalDate startDate,
        LocalDate endDate,
        Double temperature,
        Double humidity,
        Double wind,
        Double precipitations
) {
}
