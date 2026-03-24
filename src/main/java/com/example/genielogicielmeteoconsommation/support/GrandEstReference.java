package com.example.genielogicielmeteoconsommation.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GrandEstReference {

    public static final LocalDate STUDY_START_DATE = LocalDate.of(2014, 1, 1);
    public static final LocalDate STUDY_END_DATE = LocalDate.of(2014, 12, 31);

    public static final String MERGED_REGION = "Grand Est";
    public static final String FALLBACK_REGION = "France";
    public static final Set<String> HISTORICAL_REGIONS = Set.of("Alsace", "Champagne-Ardenne", "Lorraine");
    public static final Set<String> IMPORT_REGIONS = Set.of(
            MERGED_REGION,
            FALLBACK_REGION,
            "Alsace",
            "Champagne-Ardenne",
            "Lorraine"
    );

    public static final Map<String, String> DEPARTMENTS;

    static {
        LinkedHashMap<String, String> departments = new LinkedHashMap<>();
        departments.put("08", "Ardennes");
        departments.put("10", "Aube");
        departments.put("51", "Marne");
        departments.put("52", "Haute-Marne");
        departments.put("54", "Meurthe-et-Moselle");
        departments.put("55", "Meuse");
        departments.put("57", "Moselle");
        departments.put("67", "Bas-Rhin");
        departments.put("68", "Haut-Rhin");
        departments.put("88", "Vosges");
        DEPARTMENTS = Collections.unmodifiableMap(departments);
    }

    private GrandEstReference() {
    }

    public static List<String> defaultDepartments() {
        return new ArrayList<>(DEPARTMENTS.keySet());
    }

    public static String departmentLabel(String code) {
        return DEPARTMENTS.getOrDefault(code, code);
    }

    public static List<String> normalizeDepartments(Collection<String> departments) {
        if (departments == null || departments.isEmpty()) {
            return defaultDepartments();
        }

        List<String> normalized = departments.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .filter(DEPARTMENTS::containsKey)
                .distinct()
                .toList();

        return normalized.isEmpty() ? defaultDepartments() : normalized;
    }
}
