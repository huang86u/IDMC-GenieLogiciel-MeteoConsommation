package com.example.genielogicielmeteoconsommation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardOverviewResponse(
        FilterSelection filters,
        Summary summary,
        DataCoverage coverage,
        List<String> narrativeHighlights,
        List<DailyTrendPoint> dailyTrends,
        List<DepartmentProfile> departmentProfiles,
        List<DepartmentMonthlyPoint> departmentMonthlyTrends,
        List<TemperatureBucketPoint> temperatureBuckets,
        List<ScatterPoint> scatterPoints,
        List<SeasonalPoint> seasonalComparisons,
        RegressionModel regressionModel,
        Transparency transparency
) {

    public record FilterSelection(
            LocalDate startDate,
            LocalDate endDate,
            List<String> selectedDepartments,
            List<String> selectedDepartmentLabels
    ) {
    }

    public record Summary(
            boolean dataReady,
            String message,
            long hourlyObservations,
            double averageConsumptionMw,
            double averageTemperature,
            double averageHumidity,
            double averageWind,
            double averagePrecipitations,
            double correlationTemperatureConsumption,
            double coldHoursAverageConsumption,
            double warmHoursAverageConsumption,
            double coldToWarmGap,
            double peakConsumptionMw,
            double lowestTemperature,
            double highestTemperature
    ) {
    }

    public record DataCoverage(
            long consumptionRows,
            long weatherRows,
            long joinedHourlyObservations,
            long weatherStationCount,
            String periodLabel,
            List<String> importedConsumptionRegions
    ) {
    }

    public record DailyTrendPoint(
            LocalDate date,
            double averageConsumptionMw,
            double peakConsumptionMw,
            Double averageTemperature,
            Double averageHumidity,
            Double averageWind,
            Double totalPrecipitations
    ) {
    }

    public record DepartmentProfile(
            String departement,
            String label,
            double averageTemperature,
            double averageHumidity,
            double averageWind,
            double averagePrecipitations,
            double minimumTemperature,
            double maximumTemperature,
            long observationCount,
            long stationCount
    ) {
    }

    public record DepartmentMonthlyPoint(
            String departement,
            String label,
            int month,
            String monthLabel,
            double averageTemperature
    ) {
    }

    public record TemperatureBucketPoint(
            double bucketStart,
            double bucketEnd,
            double averageTemperature,
            double averageConsumptionMw,
            long observationCount
    ) {
    }

    public record ScatterPoint(
            String timestamp,
            double temperature,
            double consumptionMw,
            String season
    ) {
    }

    public record SeasonalPoint(
            String seasonCode,
            String label,
            double averageConsumptionMw,
            double averageTemperature,
            double averageHumidity,
            double averageWind,
            double totalPrecipitations,
            double correlation,
            long observationCount
    ) {
    }

    public record RegressionModel(
            boolean ready,
            String label,
            double intercept,
            Map<String, Double> coefficients,
            Map<String, Double> defaultInputs,
            double rSquared,
            long observationsUsed
    ) {
    }

    public record Transparency(
            List<String> sources,
            List<String> processingSteps,
            List<String> notes
    ) {
    }
}
