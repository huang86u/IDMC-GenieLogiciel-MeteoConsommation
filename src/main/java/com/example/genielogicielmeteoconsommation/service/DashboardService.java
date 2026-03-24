package com.example.genielogicielmeteoconsommation.service;

import com.example.genielogicielmeteoconsommation.dto.DashboardOverviewResponse;
import com.example.genielogicielmeteoconsommation.dto.EstimateRequest;
import com.example.genielogicielmeteoconsommation.dto.EstimateResponse;
import com.example.genielogicielmeteoconsommation.model.ConsommationElectrique;
import com.example.genielogicielmeteoconsommation.model.DonneesMeteo;
import com.example.genielogicielmeteoconsommation.repository.ConsommationElectriqueRepository;
import com.example.genielogicielmeteoconsommation.repository.DonneesMeteoRepository;
import com.example.genielogicielmeteoconsommation.support.GrandEstReference;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double COLD_THRESHOLD = 5.0;
    private static final double WARM_THRESHOLD = 20.0;
    private static final double TEMPERATURE_BUCKET_SIZE = 2.0;
    private static final long OVERVIEW_CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final ConsommationElectriqueRepository consommationRepository;
    private final DonneesMeteoRepository meteoRepository;
    private final Map<OverviewCacheKey, CachedOverview> overviewCache = new ConcurrentHashMap<>();

    public DashboardService(
            ConsommationElectriqueRepository consommationRepository,
            DonneesMeteoRepository meteoRepository
    ) {
        this.consommationRepository = consommationRepository;
        this.meteoRepository = meteoRepository;
    }

    public DashboardOverviewResponse buildOverview(
            Collection<String> requestedDepartments,
            LocalDate requestedStartDate,
            LocalDate requestedEndDate
    ) {
        LocalDateRange dateRange = normalizeDateRange(requestedStartDate, requestedEndDate);
        List<String> departments = GrandEstReference.normalizeDepartments(requestedDepartments);
        OverviewCacheKey cacheKey = new OverviewCacheKey(
                dateRange.startDate(),
                dateRange.endDate(),
                List.copyOf(departments)
        );
        CachedOverview cachedOverview = overviewCache.get(cacheKey);
        if (cachedOverview != null && !cachedOverview.isExpired()) {
            return cachedOverview.response();
        }

        DashboardOverviewResponse response = doBuildOverview(dateRange, departments);
        overviewCache.put(cacheKey, new CachedOverview(response, System.currentTimeMillis()));
        return response;
    }

    public void clearOverviewCache() {
        overviewCache.clear();
    }

    private DashboardOverviewResponse doBuildOverview(
            LocalDateRange dateRange,
            List<String> departments
    ) {
        List<ConsommationElectrique> consommationRows =
                consommationRepository.findAllByDateBetween(dateRange.startDate(), dateRange.endDate());
        List<DonneesMeteo> weatherRows =
                meteoRepository.findAllByDateBetweenAndDepartementIn(dateRange.startDate(), dateRange.endDate(), departments);

        OverviewData overviewData = computeOverview(consommationRows, weatherRows, dateRange);
        RegressionResult regressionResult = fitRegression(overviewData.joinedPoints());

        return new DashboardOverviewResponse(
                new DashboardOverviewResponse.FilterSelection(
                        dateRange.startDate(),
                        dateRange.endDate(),
                        departments,
                        departments.stream().map(GrandEstReference::departmentLabel).toList()
                ),
                overviewData.summary(),
                overviewData.coverage(),
                buildNarrativeHighlights(overviewData, regressionResult),
                overviewData.dailyTrends(),
                overviewData.departmentProfiles(),
                overviewData.departmentMonthlyPoints(),
                overviewData.temperatureBuckets(),
                overviewData.scatterPoints(),
                overviewData.seasonalPoints(),
                regressionResult.toResponseModel(),
                buildTransparency()
        );
    }

    public EstimateResponse estimate(EstimateRequest request) {
        EstimateRequest safeRequest = request == null
                ? new EstimateRequest(null, null, null, null, null, null, null)
                : request;

        DashboardOverviewResponse overview = buildOverview(
                safeRequest.departments(),
                safeRequest.startDate(),
                safeRequest.endDate()
        );
        DashboardOverviewResponse.RegressionModel regressionModel = overview.regressionModel();

        if (!regressionModel.ready()) {
            return new EstimateResponse(
                    false,
                    "Le modele n'est pas disponible. Importez des donnees meteo et consommation compatibles.",
                    null,
                    null,
                    Map.of(),
                    Map.of(),
                    null,
                    null
            );
        }

        Map<String, Double> inputsUsed = new LinkedHashMap<>();
        regressionModel.defaultInputs().forEach((feature, defaultValue) -> {
            Double value = switch (feature) {
                case "temperature" -> safeRequest.temperature();
                case "humidity" -> safeRequest.humidity();
                case "wind" -> safeRequest.wind();
                case "precipitations" -> safeRequest.precipitations();
                default -> defaultValue;
            };
            inputsUsed.put(feature, value != null ? value : defaultValue);
        });

        double estimatedConsumption = regressionModel.intercept();
        for (Map.Entry<String, Double> coefficient : regressionModel.coefficients().entrySet()) {
            estimatedConsumption += coefficient.getValue() * inputsUsed.getOrDefault(coefficient.getKey(), 0.0);
        }

        return new EstimateResponse(
                true,
                "Estimation calculee a partir du modele lineaire entraine sur les donnees filtrees.",
                round(estimatedConsumption),
                regressionModel.intercept(),
                regressionModel.coefficients(),
                roundMap(inputsUsed),
                regressionModel.rSquared(),
                regressionModel.observationsUsed()
        );
    }

    private OverviewData computeOverview(
            List<ConsommationElectrique> consommationRows,
            List<DonneesMeteo> weatherRows,
            LocalDateRange dateRange
    ) {
        Map<LocalDateTime, Double> regionalConsumption = buildRegionalConsumption(consommationRows);
        Map<LocalDateTime, WeatherAggregate> weatherByTimestamp = buildWeatherAggregates(weatherRows);

        List<JoinedPoint> joinedPoints = regionalConsumption.entrySet().stream()
                .filter(entry -> weatherByTimestamp.containsKey(entry.getKey()))
                .map(entry -> JoinedPoint.from(entry.getKey(), entry.getValue(), weatherByTimestamp.get(entry.getKey())))
                .sorted(Comparator.comparing(JoinedPoint::timestamp))
                .toList();

        DashboardOverviewResponse.Summary summary = buildSummary(joinedPoints);

        Set<String> importedRegions = consommationRows.stream()
                .map(ConsommationElectrique::getRegion)
                .filter(GrandEstReference.IMPORT_REGIONS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> stationNames = weatherRows.stream()
                .map(DonneesMeteo::getStation)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(HashSet::new));

        DashboardOverviewResponse.DataCoverage coverage = new DashboardOverviewResponse.DataCoverage(
                consommationRows.size(),
                weatherRows.size(),
                joinedPoints.size(),
                stationNames.size(),
                dateRange.startDate().format(PERIOD_FORMAT) + " -> " + dateRange.endDate().format(PERIOD_FORMAT),
                importedRegions.stream().sorted().toList()
        );

        return new OverviewData(
                joinedPoints,
                summary,
                coverage,
                buildDailyTrends(joinedPoints),
                buildDepartmentProfiles(weatherRows),
                buildDepartmentMonthlyPoints(weatherRows),
                buildTemperatureBuckets(joinedPoints),
                buildScatterPoints(joinedPoints),
                buildSeasonalPoints(joinedPoints)
        );
    }

    private Map<LocalDateTime, Double> buildRegionalConsumption(List<ConsommationElectrique> consommationRows) {
        Map<LocalDateTime, Map<String, Double>> valuesByTimestamp = new TreeMap<>();

        for (ConsommationElectrique consommation : consommationRows) {
            if (consommation.getDate() == null
                    || consommation.getHeure() == null
                    || consommation.getConsommationMw() == null
                    || !GrandEstReference.IMPORT_REGIONS.contains(consommation.getRegion())) {
                continue;
            }

            LocalDateTime timestamp = LocalDateTime.of(consommation.getDate(), consommation.getHeure());
            valuesByTimestamp
                    .computeIfAbsent(timestamp, key -> new LinkedHashMap<>())
                    .merge(consommation.getRegion(), consommation.getConsommationMw(), Double::sum);
        }

        Map<LocalDateTime, Double> resolvedConsumption = new TreeMap<>();
        for (Map.Entry<LocalDateTime, Map<String, Double>> entry : valuesByTimestamp.entrySet()) {
            Double resolvedValue = resolveRegionalConsumption(entry.getValue());
            if (resolvedValue != null) {
                resolvedConsumption.put(entry.getKey(), resolvedValue);
            }
        }

        return resolvedConsumption;
    }

    private Double resolveRegionalConsumption(Map<String, Double> valuesByRegion) {
        boolean hasHistoricalRegions = GrandEstReference.HISTORICAL_REGIONS.stream().anyMatch(valuesByRegion::containsKey);
        if (hasHistoricalRegions) {
            return GrandEstReference.HISTORICAL_REGIONS.stream()
                    .filter(valuesByRegion::containsKey)
                    .mapToDouble(valuesByRegion::get)
                    .sum();
        }
        if (valuesByRegion.containsKey(GrandEstReference.MERGED_REGION)) {
            return valuesByRegion.get(GrandEstReference.MERGED_REGION);
        }
        if (valuesByRegion.containsKey(GrandEstReference.FALLBACK_REGION)) {
            return valuesByRegion.get(GrandEstReference.FALLBACK_REGION);
        }
        return null;
    }

    private Map<LocalDateTime, WeatherAggregate> buildWeatherAggregates(List<DonneesMeteo> weatherRows) {
        Map<LocalDateTime, WeatherAccumulator> accumulators = new TreeMap<>();

        for (DonneesMeteo weatherRow : weatherRows) {
            if (weatherRow.getDate() == null || weatherRow.getHeure() == null) {
                continue;
            }

            LocalDateTime timestamp = LocalDateTime.of(weatherRow.getDate(), weatherRow.getHeure());
            accumulators.computeIfAbsent(timestamp, key -> new WeatherAccumulator()).add(weatherRow);
        }

        Map<LocalDateTime, WeatherAggregate> result = new TreeMap<>();
        accumulators.forEach((timestamp, accumulator) -> result.put(timestamp, accumulator.toAggregate()));
        return result;
    }

    private DashboardOverviewResponse.Summary buildSummary(List<JoinedPoint> joinedPoints) {
        if (joinedPoints.isEmpty()) {
            return new DashboardOverviewResponse.Summary(
                    false,
                    "Aucune serie commune n'est disponible pour l'analyse. Importez les donnees RTE et Meteo-France.",
                    0L,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        NumericAccumulator consumptionValues = new NumericAccumulator();
        NumericAccumulator temperatureValues = new NumericAccumulator();
        NumericAccumulator humidityValues = new NumericAccumulator();
        NumericAccumulator windValues = new NumericAccumulator();
        NumericAccumulator precipitationValues = new NumericAccumulator();
        NumericAccumulator coldConsumptions = new NumericAccumulator();
        NumericAccumulator warmConsumptions = new NumericAccumulator();

        List<Double> temperatures = new ArrayList<>();
        List<Double> consumptionsForCorrelation = new ArrayList<>();

        for (JoinedPoint joinedPoint : joinedPoints) {
            consumptionValues.add(joinedPoint.consumptionMw());

            if (joinedPoint.temperature() != null) {
                temperatureValues.add(joinedPoint.temperature());
                temperatures.add(joinedPoint.temperature());
                consumptionsForCorrelation.add(joinedPoint.consumptionMw());

                if (joinedPoint.temperature() <= COLD_THRESHOLD) {
                    coldConsumptions.add(joinedPoint.consumptionMw());
                }
                if (joinedPoint.temperature() >= WARM_THRESHOLD) {
                    warmConsumptions.add(joinedPoint.consumptionMw());
                }
            }
            if (joinedPoint.humidity() != null) {
                humidityValues.add(joinedPoint.humidity());
            }
            if (joinedPoint.wind() != null) {
                windValues.add(joinedPoint.wind());
            }
            if (joinedPoint.precipitations() != null) {
                precipitationValues.add(joinedPoint.precipitations());
            }
        }

        double coldAverage = coldConsumptions.hasValues() ? coldConsumptions.average() : consumptionValues.average();
        double warmAverage = warmConsumptions.hasValues() ? warmConsumptions.average() : consumptionValues.average();

        return new DashboardOverviewResponse.Summary(
                true,
                "Analyse calculee sur les observations horaires communes entre consommation et meteo.",
                joinedPoints.size(),
                round(consumptionValues.average()),
                round(temperatureValues.average()),
                round(humidityValues.average()),
                round(windValues.average()),
                round(precipitationValues.average()),
                round(pearsonCorrelation(temperatures, consumptionsForCorrelation)),
                round(coldAverage),
                round(warmAverage),
                round(coldAverage - warmAverage),
                round(consumptionValues.max()),
                round(temperatureValues.min()),
                round(temperatureValues.max())
        );
    }

    private List<DashboardOverviewResponse.DailyTrendPoint> buildDailyTrends(List<JoinedPoint> joinedPoints) {
        Map<LocalDate, DailyAccumulator> accumulators = new TreeMap<>();

        for (JoinedPoint joinedPoint : joinedPoints) {
            accumulators.computeIfAbsent(joinedPoint.timestamp().toLocalDate(), key -> new DailyAccumulator()).add(joinedPoint);
        }

        return accumulators.entrySet().stream()
                .map(entry -> new DashboardOverviewResponse.DailyTrendPoint(
                        entry.getKey(),
                        round(entry.getValue().averageConsumption()),
                        round(entry.getValue().peakConsumption()),
                        roundNullable(entry.getValue().averageTemperature()),
                        roundNullable(entry.getValue().averageHumidity()),
                        roundNullable(entry.getValue().averageWind()),
                        roundNullable(entry.getValue().totalPrecipitations())
                ))
                .toList();
    }

    private List<DashboardOverviewResponse.DepartmentProfile> buildDepartmentProfiles(List<DonneesMeteo> weatherRows) {
        Map<String, WeatherAccumulator> byDepartment = new LinkedHashMap<>();

        for (DonneesMeteo weatherRow : weatherRows) {
            if (weatherRow.getDepartement() == null) {
                continue;
            }
            byDepartment.computeIfAbsent(weatherRow.getDepartement(), key -> new WeatherAccumulator()).add(weatherRow);
        }

        return byDepartment.entrySet().stream()
                .filter(entry -> GrandEstReference.DEPARTMENTS.containsKey(entry.getKey()))
                .map(entry -> new DashboardOverviewResponse.DepartmentProfile(
                        entry.getKey(),
                        GrandEstReference.departmentLabel(entry.getKey()),
                        round(entry.getValue().temperatureAverage()),
                        round(entry.getValue().humidityAverage()),
                        round(entry.getValue().windAverage()),
                        round(entry.getValue().precipitationAverage()),
                        round(entry.getValue().temperatureMin()),
                        round(entry.getValue().temperatureMax()),
                        entry.getValue().observationCount(),
                        entry.getValue().stationCount()
                ))
                .sorted(Comparator.comparing(DashboardOverviewResponse.DepartmentProfile::departement))
                .toList();
    }

    private List<DashboardOverviewResponse.DepartmentMonthlyPoint> buildDepartmentMonthlyPoints(List<DonneesMeteo> weatherRows) {
        Map<String, Map<Integer, NumericAccumulator>> values = new LinkedHashMap<>();

        for (DonneesMeteo weatherRow : weatherRows) {
            if (weatherRow.getDepartement() == null
                    || weatherRow.getDate() == null
                    || weatherRow.getTemperature() == null) {
                continue;
            }

            values.computeIfAbsent(weatherRow.getDepartement(), key -> new TreeMap<>())
                    .computeIfAbsent(weatherRow.getDate().getMonthValue(), key -> new NumericAccumulator())
                    .add(weatherRow.getTemperature());
        }

        List<DashboardOverviewResponse.DepartmentMonthlyPoint> result = new ArrayList<>();
        values.forEach((department, monthlyValues) -> monthlyValues.forEach((month, accumulator) ->
                result.add(new DashboardOverviewResponse.DepartmentMonthlyPoint(
                        department,
                        GrandEstReference.departmentLabel(department),
                        month,
                        monthLabel(month),
                        round(accumulator.average())
                ))
        ));

        return result.stream()
                .sorted(Comparator.comparing(DashboardOverviewResponse.DepartmentMonthlyPoint::departement)
                        .thenComparing(DashboardOverviewResponse.DepartmentMonthlyPoint::month))
                .toList();
    }

    private List<DashboardOverviewResponse.TemperatureBucketPoint> buildTemperatureBuckets(List<JoinedPoint> joinedPoints) {
        Map<Double, BucketAccumulator> buckets = new TreeMap<>();

        for (JoinedPoint joinedPoint : joinedPoints) {
            if (joinedPoint.temperature() == null) {
                continue;
            }

            double bucketStart = Math.floor(joinedPoint.temperature() / TEMPERATURE_BUCKET_SIZE) * TEMPERATURE_BUCKET_SIZE;
            buckets.computeIfAbsent(bucketStart, key -> new BucketAccumulator()).add(joinedPoint);
        }

        return buckets.entrySet().stream()
                .map(entry -> new DashboardOverviewResponse.TemperatureBucketPoint(
                        round(entry.getKey()),
                        round(entry.getKey() + TEMPERATURE_BUCKET_SIZE),
                        round(entry.getValue().averageTemperature()),
                        round(entry.getValue().averageConsumption()),
                        entry.getValue().observationCount()
                ))
                .toList();
    }

    private List<DashboardOverviewResponse.ScatterPoint> buildScatterPoints(List<JoinedPoint> joinedPoints) {
        return joinedPoints.stream()
                .filter(point -> point.temperature() != null)
                .map(point -> new DashboardOverviewResponse.ScatterPoint(
                        point.timestamp().format(TIMESTAMP_FORMAT),
                        round(point.temperature()),
                        round(point.consumptionMw()),
                        seasonLabel(point.seasonCode())
                ))
                .toList();
    }

    private List<DashboardOverviewResponse.SeasonalPoint> buildSeasonalPoints(List<JoinedPoint> joinedPoints) {
        Map<String, SeasonAccumulator> accumulators = new LinkedHashMap<>();
        for (String season : List.of("WINTER", "SPRING", "SUMMER", "AUTUMN")) {
            accumulators.put(season, new SeasonAccumulator());
        }

        for (JoinedPoint joinedPoint : joinedPoints) {
            accumulators.computeIfAbsent(joinedPoint.seasonCode(), key -> new SeasonAccumulator()).add(joinedPoint);
        }

        return accumulators.entrySet().stream()
                .map(entry -> new DashboardOverviewResponse.SeasonalPoint(
                        entry.getKey(),
                        seasonLabel(entry.getKey()),
                        round(entry.getValue().averageConsumption()),
                        round(entry.getValue().averageTemperature()),
                        round(entry.getValue().averageHumidity()),
                        round(entry.getValue().averageWind()),
                        round(entry.getValue().totalPrecipitations()),
                        round(entry.getValue().temperatureCorrelation()),
                        entry.getValue().observationCount()
                ))
                .toList();
    }

    private List<String> buildNarrativeHighlights(OverviewData overviewData, RegressionResult regressionResult) {
        DashboardOverviewResponse.Summary summary = overviewData.summary();
        if (!summary.dataReady()) {
            return List.of("Importez les donnees puis rechargez le tableau de bord pour generer les analyses.");
        }

        List<String> highlights = new ArrayList<>();
        highlights.add("La relation temperature/consommation est "
                + describeCorrelation(summary.correlationTemperatureConsumption())
                + " (coefficient de Pearson " + summary.correlationTemperatureConsumption() + ").");
        highlights.add("Les heures froides affichent en moyenne "
                + summary.coldHoursAverageConsumption() + " MW contre "
                + summary.warmHoursAverageConsumption() + " MW pour les heures chaudes.");

        overviewData.seasonalPoints().stream()
                .max(Comparator.comparing(DashboardOverviewResponse.SeasonalPoint::averageConsumptionMw))
                .ifPresent(point -> highlights.add("La saison la plus consommatrice dans le filtre courant est "
                        + point.label() + " avec " + point.averageConsumptionMw() + " MW en moyenne."));

        if (regressionResult.ready()) {
            highlights.add("Le modele d'estimation explique "
                    + round(regressionResult.rSquared() * 100.0)
                    + "% de la variance sur l'echantillon utilise.");
        }

        return highlights;
    }

    private DashboardOverviewResponse.Transparency buildTransparency() {
        return new DashboardOverviewResponse.Transparency(
                List.of(
                        "RTE eCO2mix - consommation electrique regionale",
                        "Meteo-France - donnees climatologiques horaires"
                ),
                List.of(
                        "Import des fichiers CSV et nettoyage des valeurs manquantes.",
                        "Filtrage sur l'annee 2014 et sur les departements du Grand Est.",
                        "Conversion des horodatages meteo depuis UTC vers Europe/Paris.",
                        "Aggregation horaire de la meteo et reconstruction de la consommation Grand Est.",
                        "Calcul des indicateurs: correlation, saisons, tranches de temperature et estimation."
                ),
                List.of(
                        "La consommation regionale est reconstruite a partir d'Alsace, Champagne-Ardenne et Lorraine lorsque ces series existent.",
                        "Le filtre departement agit sur les indicateurs meteo, la consommation reste regionale.",
                        "L'estimation repose sur une regression lineaire simple adaptee au filtre courant."
                )
        );
    }

    private LocalDateRange normalizeDateRange(LocalDate requestedStartDate, LocalDate requestedEndDate) {
        LocalDate startDate = requestedStartDate != null ? requestedStartDate : GrandEstReference.STUDY_START_DATE;
        LocalDate endDate = requestedEndDate != null ? requestedEndDate : GrandEstReference.STUDY_END_DATE;

        if (startDate.isBefore(GrandEstReference.STUDY_START_DATE)) {
            startDate = GrandEstReference.STUDY_START_DATE;
        }
        if (endDate.isAfter(GrandEstReference.STUDY_END_DATE)) {
            endDate = GrandEstReference.STUDY_END_DATE;
        }
        if (endDate.isBefore(startDate)) {
            LocalDate temporary = startDate;
            startDate = endDate;
            endDate = temporary;
        }

        return new LocalDateRange(startDate, endDate);
    }

    private RegressionResult fitRegression(List<JoinedPoint> joinedPoints) {
        List<FeatureDefinition> features = List.of(
                new FeatureDefinition("temperature", JoinedPoint::temperature),
                new FeatureDefinition("humidity", JoinedPoint::humidity),
                new FeatureDefinition("wind", JoinedPoint::wind),
                new FeatureDefinition("precipitations", JoinedPoint::precipitations)
        );

        for (int featureCount = features.size(); featureCount >= 1; featureCount--) {
            List<FeatureDefinition> activeFeatures = features.subList(0, featureCount);
            List<RegressionSample> samples = extractRegressionSamples(joinedPoints, activeFeatures);
            if (samples.size() < activeFeatures.size() + 5) {
                continue;
            }

            RegressionResult result = performRegression(samples, activeFeatures);
            if (result.ready()) {
                return result;
            }
        }

        return RegressionResult.unavailable();
    }

    private List<RegressionSample> extractRegressionSamples(
            List<JoinedPoint> joinedPoints,
            List<FeatureDefinition> activeFeatures
    ) {
        List<RegressionSample> samples = new ArrayList<>();

        for (JoinedPoint joinedPoint : joinedPoints) {
            double[] values = new double[activeFeatures.size()];
            boolean valid = true;

            for (int index = 0; index < activeFeatures.size(); index++) {
                Double featureValue = activeFeatures.get(index).extractor().apply(joinedPoint);
                if (featureValue == null) {
                    valid = false;
                    break;
                }
                values[index] = featureValue;
            }

            if (valid) {
                samples.add(new RegressionSample(values, joinedPoint.consumptionMw()));
            }
        }

        return samples;
    }

    private RegressionResult performRegression(List<RegressionSample> samples, List<FeatureDefinition> activeFeatures) {
        int parameterCount = activeFeatures.size() + 1;
        double[][] xtx = new double[parameterCount][parameterCount];
        double[] xty = new double[parameterCount];

        for (RegressionSample sample : samples) {
            double[] x = new double[parameterCount];
            x[0] = 1.0;
            System.arraycopy(sample.features(), 0, x, 1, sample.features().length);

            for (int row = 0; row < parameterCount; row++) {
                xty[row] += x[row] * sample.target();
                for (int column = 0; column < parameterCount; column++) {
                    xtx[row][column] += x[row] * x[column];
                }
            }
        }

        for (int diagonal = 0; diagonal < parameterCount; diagonal++) {
            xtx[diagonal][diagonal] += 1.0e-6;
        }

        double[] coefficientsVector = solveLinearSystem(xtx, xty);
        if (coefficientsVector == null) {
            return RegressionResult.unavailable();
        }

        double intercept = coefficientsVector[0];
        Map<String, Double> coefficients = new LinkedHashMap<>();
        Map<String, Double> defaultInputs = new LinkedHashMap<>();

        for (int index = 0; index < activeFeatures.size(); index++) {
            coefficients.put(activeFeatures.get(index).name(), coefficientsVector[index + 1]);
            defaultInputs.put(activeFeatures.get(index).name(), mean(samples, index));
        }

        double meanTarget = samples.stream().mapToDouble(RegressionSample::target).average().orElse(0.0);
        double residualSumSquares = 0.0;
        double totalSumSquares = 0.0;

        for (RegressionSample sample : samples) {
            double prediction = intercept;
            for (int index = 0; index < sample.features().length; index++) {
                prediction += coefficientsVector[index + 1] * sample.features()[index];
            }
            residualSumSquares += Math.pow(sample.target() - prediction, 2);
            totalSumSquares += Math.pow(sample.target() - meanTarget, 2);
        }

        double rSquared = totalSumSquares == 0.0 ? 0.0 : 1.0 - (residualSumSquares / totalSumSquares);
        String label = "Regression lineaire sur "
                + activeFeatures.stream().map(FeatureDefinition::name).collect(Collectors.joining(", "));

        return new RegressionResult(true, label, intercept, coefficients, defaultInputs, rSquared, samples.size());
    }

    private double[] solveLinearSystem(double[][] matrix, double[] vector) {
        int size = vector.length;
        double[][] augmented = new double[size][size + 1];

        for (int row = 0; row < size; row++) {
            System.arraycopy(matrix[row], 0, augmented[row], 0, size);
            augmented[row][size] = vector[row];
        }

        for (int pivot = 0; pivot < size; pivot++) {
            int maxRow = pivot;
            for (int row = pivot + 1; row < size; row++) {
                if (Math.abs(augmented[row][pivot]) > Math.abs(augmented[maxRow][pivot])) {
                    maxRow = row;
                }
            }

            if (Math.abs(augmented[maxRow][pivot]) < 1.0e-10) {
                return null;
            }

            double[] temporary = augmented[pivot];
            augmented[pivot] = augmented[maxRow];
            augmented[maxRow] = temporary;

            double pivotValue = augmented[pivot][pivot];
            for (int column = pivot; column <= size; column++) {
                augmented[pivot][column] /= pivotValue;
            }

            for (int row = 0; row < size; row++) {
                if (row == pivot) {
                    continue;
                }

                double factor = augmented[row][pivot];
                for (int column = pivot; column <= size; column++) {
                    augmented[row][column] -= factor * augmented[pivot][column];
                }
            }
        }

        double[] solution = new double[size];
        for (int row = 0; row < size; row++) {
            solution[row] = augmented[row][size];
        }
        return solution;
    }

    private double mean(List<RegressionSample> samples, int featureIndex) {
        return samples.stream().mapToDouble(sample -> sample.features()[featureIndex]).average().orElse(0.0);
    }

    private static double pearsonCorrelation(List<Double> xValues, List<Double> yValues) {
        if (xValues.size() < 2 || xValues.size() != yValues.size()) {
            return 0.0;
        }

        double meanX = xValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = yValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double numerator = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;

        for (int index = 0; index < xValues.size(); index++) {
            double centeredX = xValues.get(index) - meanX;
            double centeredY = yValues.get(index) - meanY;
            numerator += centeredX * centeredY;
            denominatorX += centeredX * centeredX;
            denominatorY += centeredY * centeredY;
        }

        double denominator = Math.sqrt(denominatorX * denominatorY);
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private String describeCorrelation(double correlation) {
        double absoluteCorrelation = Math.abs(correlation);
        if (absoluteCorrelation >= 0.7) {
            return correlation < 0 ? "fortement negative" : "fortement positive";
        }
        if (absoluteCorrelation >= 0.4) {
            return correlation < 0 ? "nettement negative" : "nettement positive";
        }
        if (absoluteCorrelation >= 0.2) {
            return correlation < 0 ? "legerement negative" : "legerement positive";
        }
        return "faible";
    }

    private static String seasonLabel(String seasonCode) {
        return switch (seasonCode) {
            case "WINTER" -> "Hiver";
            case "SPRING" -> "Printemps";
            case "SUMMER" -> "Ete";
            case "AUTUMN" -> "Automne";
            default -> seasonCode;
        };
    }

    private static String monthLabel(int monthNumber) {
        return switch (Month.of(monthNumber)) {
            case JANUARY -> "Jan";
            case FEBRUARY -> "Fev";
            case MARCH -> "Mar";
            case APRIL -> "Avr";
            case MAY -> "Mai";
            case JUNE -> "Juin";
            case JULY -> "Juil";
            case AUGUST -> "Aou";
            case SEPTEMBER -> "Sep";
            case OCTOBER -> "Oct";
            case NOVEMBER -> "Nov";
            case DECEMBER -> "Dec";
        };
    }

    private static Double roundNullable(Double value) {
        return value == null ? null : round(value);
    }

    private static Map<String, Double> roundMap(Map<String, Double> values) {
        Map<String, Double> rounded = new LinkedHashMap<>();
        values.forEach((key, value) -> rounded.put(key, round(value)));
        return rounded;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record LocalDateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record OverviewCacheKey(LocalDate startDate, LocalDate endDate, List<String> departments) {
    }

    private record CachedOverview(DashboardOverviewResponse response, long cachedAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAtMillis > OVERVIEW_CACHE_TTL_MILLIS;
        }
    }

    private record OverviewData(
            List<JoinedPoint> joinedPoints,
            DashboardOverviewResponse.Summary summary,
            DashboardOverviewResponse.DataCoverage coverage,
            List<DashboardOverviewResponse.DailyTrendPoint> dailyTrends,
            List<DashboardOverviewResponse.DepartmentProfile> departmentProfiles,
            List<DashboardOverviewResponse.DepartmentMonthlyPoint> departmentMonthlyPoints,
            List<DashboardOverviewResponse.TemperatureBucketPoint> temperatureBuckets,
            List<DashboardOverviewResponse.ScatterPoint> scatterPoints,
            List<DashboardOverviewResponse.SeasonalPoint> seasonalPoints
    ) {
    }

    private record WeatherAggregate(
            Double temperature,
            Double humidity,
            Double precipitations,
            Double wind
    ) {
    }

    private record JoinedPoint(
            LocalDateTime timestamp,
            double consumptionMw,
            Double temperature,
            Double humidity,
            Double precipitations,
            Double wind,
            String seasonCode
    ) {
        private static JoinedPoint from(LocalDateTime timestamp, double consumptionMw, WeatherAggregate aggregate) {
            return new JoinedPoint(
                    timestamp,
                    consumptionMw,
                    aggregate.temperature(),
                    aggregate.humidity(),
                    aggregate.precipitations(),
                    aggregate.wind(),
                    DashboardService.seasonCode(timestamp.toLocalDate())
            );
        }
    }

    private static String seasonCode(LocalDate date) {
        int month = date.getMonthValue();
        if (month == 12 || month == 1 || month == 2) {
            return "WINTER";
        }
        if (month <= 5) {
            return "SPRING";
        }
        if (month <= 8) {
            return "SUMMER";
        }
        return "AUTUMN";
    }

    private record FeatureDefinition(String name, FeatureExtractor extractor) {
    }

    private record RegressionSample(double[] features, double target) {
    }

    private record RegressionResult(
            boolean ready,
            String label,
            double intercept,
            Map<String, Double> coefficients,
            Map<String, Double> defaultInputs,
            double rSquared,
            long observationsUsed
    ) {
        private static RegressionResult unavailable() {
            return new RegressionResult(false, "Modele indisponible", 0.0, Map.of(), Map.of(), 0.0, 0L);
        }

        private DashboardOverviewResponse.RegressionModel toResponseModel() {
            return new DashboardOverviewResponse.RegressionModel(
                    ready,
                    label,
                    round(intercept),
                    roundMap(coefficients),
                    roundMap(defaultInputs),
                    round(rSquared),
                    observationsUsed
            );
        }
    }

    @FunctionalInterface
    private interface FeatureExtractor {
        Double apply(JoinedPoint point);
    }

    private static final class NumericAccumulator {

        private double sum;
        private long count;
        private double min = Double.POSITIVE_INFINITY;
        private double max = Double.NEGATIVE_INFINITY;

        private void add(Double value) {
            if (value == null) {
                return;
            }

            sum += value;
            count++;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        private boolean hasValues() {
            return count > 0;
        }

        private double average() {
            return count == 0 ? 0.0 : sum / count;
        }

        private double min() {
            return count == 0 ? 0.0 : min;
        }

        private double max() {
            return count == 0 ? 0.0 : max;
        }

        private double sum() {
            return sum;
        }
    }

    private static final class WeatherAccumulator {

        private final NumericAccumulator temperatures = new NumericAccumulator();
        private final NumericAccumulator humidities = new NumericAccumulator();
        private final NumericAccumulator precipitations = new NumericAccumulator();
        private final NumericAccumulator winds = new NumericAccumulator();
        private final Set<String> stations = new HashSet<>();
        private long observationCount;

        private void add(DonneesMeteo weatherRow) {
            observationCount++;
            if (weatherRow.getStation() != null && !weatherRow.getStation().isBlank()) {
                stations.add(weatherRow.getStation());
            }

            temperatures.add(weatherRow.getTemperature());
            humidities.add(weatherRow.getHumidite());
            precipitations.add(weatherRow.getPrecipitations());
            winds.add(weatherRow.getVent());
        }

        private WeatherAggregate toAggregate() {
            return new WeatherAggregate(
                    temperatures.hasValues() ? temperatures.average() : null,
                    humidities.hasValues() ? humidities.average() : null,
                    precipitations.hasValues() ? precipitations.average() : null,
                    winds.hasValues() ? winds.average() : null
            );
        }

        private double temperatureAverage() {
            return temperatures.average();
        }

        private double humidityAverage() {
            return humidities.average();
        }

        private double precipitationAverage() {
            return precipitations.average();
        }

        private double windAverage() {
            return winds.average();
        }

        private double temperatureMin() {
            return temperatures.min();
        }

        private double temperatureMax() {
            return temperatures.max();
        }

        private long observationCount() {
            return observationCount;
        }

        private long stationCount() {
            return stations.size();
        }
    }

    private static final class DailyAccumulator {

        private final NumericAccumulator consumptions = new NumericAccumulator();
        private final NumericAccumulator temperatures = new NumericAccumulator();
        private final NumericAccumulator humidities = new NumericAccumulator();
        private final NumericAccumulator winds = new NumericAccumulator();
        private final NumericAccumulator precipitations = new NumericAccumulator();

        private void add(JoinedPoint point) {
            consumptions.add(point.consumptionMw());
            temperatures.add(point.temperature());
            humidities.add(point.humidity());
            winds.add(point.wind());
            precipitations.add(point.precipitations());
        }

        private double averageConsumption() {
            return consumptions.average();
        }

        private double peakConsumption() {
            return consumptions.max();
        }

        private Double averageTemperature() {
            return temperatures.hasValues() ? temperatures.average() : null;
        }

        private Double averageHumidity() {
            return humidities.hasValues() ? humidities.average() : null;
        }

        private Double averageWind() {
            return winds.hasValues() ? winds.average() : null;
        }

        private Double totalPrecipitations() {
            return precipitations.hasValues() ? precipitations.sum() : null;
        }
    }

    private static final class BucketAccumulator {

        private final NumericAccumulator temperatures = new NumericAccumulator();
        private final NumericAccumulator consumptions = new NumericAccumulator();

        private void add(JoinedPoint point) {
            temperatures.add(point.temperature());
            consumptions.add(point.consumptionMw());
        }

        private double averageTemperature() {
            return temperatures.average();
        }

        private double averageConsumption() {
            return consumptions.average();
        }

        private long observationCount() {
            return consumptions.count;
        }
    }

    private static final class SeasonAccumulator {

        private final NumericAccumulator temperatures = new NumericAccumulator();
        private final NumericAccumulator humidities = new NumericAccumulator();
        private final NumericAccumulator winds = new NumericAccumulator();
        private final NumericAccumulator precipitations = new NumericAccumulator();
        private final NumericAccumulator consumptions = new NumericAccumulator();
        private final List<Double> temperaturesForCorrelation = new ArrayList<>();
        private final List<Double> consumptionsForCorrelation = new ArrayList<>();

        private void add(JoinedPoint point) {
            consumptions.add(point.consumptionMw());

            if (point.temperature() != null) {
                temperatures.add(point.temperature());
                temperaturesForCorrelation.add(point.temperature());
                consumptionsForCorrelation.add(point.consumptionMw());
            }
            if (point.humidity() != null) {
                humidities.add(point.humidity());
            }
            if (point.wind() != null) {
                winds.add(point.wind());
            }
            if (point.precipitations() != null) {
                precipitations.add(point.precipitations());
            }
        }

        private double averageConsumption() {
            return consumptions.average();
        }

        private double averageTemperature() {
            return temperatures.average();
        }

        private double averageHumidity() {
            return humidities.average();
        }

        private double averageWind() {
            return winds.average();
        }

        private double totalPrecipitations() {
            return precipitations.sum();
        }

        private double temperatureCorrelation() {
            return pearsonCorrelation(temperaturesForCorrelation, consumptionsForCorrelation);
        }

        private long observationCount() {
            return consumptions.count;
        }
    }
}
