package com.example.genielogicielmeteoconsommation.controller;

import com.example.genielogicielmeteoconsommation.dto.DashboardOverviewResponse;
import com.example.genielogicielmeteoconsommation.dto.EstimateRequest;
import com.example.genielogicielmeteoconsommation.dto.EstimateResponse;
import com.example.genielogicielmeteoconsommation.service.DashboardService;
import com.example.genielogicielmeteoconsommation.support.GrandEstReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse overview(
            @RequestParam(name = "departments", required = false) String departments,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return dashboardService.buildOverview(parseDepartments(departments), startDate, endDate);
    }

    @GetMapping("/departments")
    public List<DepartmentOption> departments() {
        return GrandEstReference.defaultDepartments().stream()
                .map(code -> new DepartmentOption(code, GrandEstReference.departmentLabel(code)))
                .toList();
    }

    @PostMapping("/estimate")
    public EstimateResponse estimate(@RequestBody(required = false) EstimateRequest request) {
        return dashboardService.estimate(request);
    }

    private List<String> parseDepartments(String rawDepartments) {
        if (rawDepartments == null || rawDepartments.isBlank()) {
            return GrandEstReference.defaultDepartments();
        }
        return Arrays.stream(rawDepartments.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public record DepartmentOption(String code, String label) {
    }
}
