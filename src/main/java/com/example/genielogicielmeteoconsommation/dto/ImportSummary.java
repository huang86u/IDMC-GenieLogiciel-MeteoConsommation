package com.example.genielogicielmeteoconsommation.dto;

public record ImportSummary(String source, String fileName, int insertedRows, int skippedRows) {
}
