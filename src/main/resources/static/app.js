const DEFAULT_START_DATE = "2014-01-01";
const DEFAULT_END_DATE = "2014-12-31";
const OVERVIEW_CACHE_TTL_MS = 10 * 60 * 1000;
const STORAGE_KEYS = {
    departments: "dashboard.departments",
    filters: "dashboard.filters",
    overviewPrefix: "dashboard.overview."
};
const GRAND_EST_MAP_DEPARTMENTS = [
    {
        code: "08",
        tone: "tone-blue",
        labelX: 182,
        labelY: 96,
        labelLines: ["Ardennes"],
        path: "M 142 28 L 186 20 L 214 36 L 236 40 L 230 70 L 236 104 L 220 126 L 226 150 L 188 162 L 154 154 L 132 126 L 136 90 Z"
    },
    {
        code: "51",
        tone: "tone-red",
        labelX: 126,
        labelY: 184,
        labelLines: ["Marne"],
        path: "M 56 138 L 106 130 L 154 154 L 188 162 L 198 194 L 192 236 L 164 252 L 170 282 L 124 292 L 84 286 L 54 256 L 42 216 L 46 168 Z"
    },
    {
        code: "10",
        tone: "tone-green",
        labelX: 116,
        labelY: 306,
        labelLines: ["Aube"],
        path: "M 56 288 L 124 292 L 170 282 L 184 314 L 178 360 L 150 392 L 100 398 L 58 376 L 36 334 L 40 306 Z"
    },
    {
        code: "55",
        tone: "tone-green",
        labelX: 258,
        labelY: 166,
        labelLines: ["Meuse"],
        path: "M 198 118 L 224 94 L 262 88 L 300 96 L 318 136 L 314 182 L 322 220 L 300 246 L 258 244 L 224 226 L 212 188 L 214 150 Z"
    },
    {
        code: "52",
        tone: "tone-gold",
        labelX: 248,
        labelY: 348,
        labelLines: ["Haute-Marne"],
        path: "M 182 306 L 230 292 L 274 286 L 302 306 L 302 354 L 286 394 L 248 408 L 206 398 L 176 370 L 170 330 Z"
    },
    {
        code: "54",
        tone: "tone-red",
        labelX: 342,
        labelY: 186,
        labelLines: ["Meurthe-", "et-Moselle"],
        path: "M 306 154 L 342 138 L 378 142 L 404 162 L 398 190 L 410 212 L 390 234 L 344 238 L 314 228 L 296 206 L 296 178 Z"
    },
    {
        code: "57",
        tone: "tone-gold",
        labelX: 420,
        labelY: 112,
        labelLines: ["Moselle"],
        path: "M 310 88 L 350 60 L 398 54 L 444 64 L 486 80 L 506 108 L 498 144 L 474 166 L 478 192 L 446 210 L 404 204 L 384 182 L 398 190 L 404 162 L 378 142 L 342 138 L 320 122 Z"
    },
    {
        code: "67",
        tone: "tone-green",
        labelX: 468,
        labelY: 180,
        labelLines: ["Bas-Rhin"],
        path: "M 444 166 L 482 158 L 528 166 L 540 192 L 530 224 L 502 246 L 474 250 L 450 234 L 442 204 Z"
    },
    {
        code: "68",
        tone: "tone-gold",
        labelX: 458,
        labelY: 346,
        labelLines: ["Haut-Rhin"],
        path: "M 430 292 L 462 286 L 486 298 L 500 324 L 500 362 L 490 404 L 466 430 L 438 426 L 416 398 L 412 350 L 418 318 Z"
    },
    {
        code: "88",
        tone: "tone-blue",
        labelX: 372,
        labelY: 278,
        labelLines: ["Vosges"],
        path: "M 308 236 L 346 238 L 390 234 L 430 236 L 442 258 L 438 292 L 430 322 L 406 340 L 368 340 L 330 332 L 306 304 L 300 268 Z"
    }
];

const state = {
    departments: [],
    overview: null,
    deselected: false,
    mapPreviewDepartment: null,
    loading: false
};

document.addEventListener("DOMContentLoaded", () => {
    wireEvents();
    initialize().catch((error) => {
        console.error(error);
        showToast("Impossible de charger le tableau de bord.", "error");
    });
});

async function initialize() {
    await loadDepartments();
    renderGrandEstMap();
    ensureFilterStatusBar();
    restoreAppliedFilters();
    renderDepartmentMapSelection();
    renderFilterStatus();

    const cachedOverview = readOverviewCache(buildFilterState(), true);
    if (cachedOverview?.data) {
        state.overview = cachedOverview.data;
        renderOverview();

        if (cachedOverview.stale) {
            await refreshOverview({ force: true, silent: true });
        }
        return;
    }

    await refreshOverview({ force: true });
}

function wireEvents() {
    document.getElementById("applyFilters")?.addEventListener("click", () => refreshOverview({ force: true }));
    document.getElementById("resetFilters")?.addEventListener("click", resetFilters);
    document.getElementById("resetMapFocus")?.addEventListener("click", focusAllDepartmentsFromMap);
    document.getElementById("toggleDepartments")?.addEventListener("click", toggleDepartments);
    document.getElementById("departmentChecklist")?.addEventListener("change", () => {
        state.mapPreviewDepartment = null;
        syncDepartmentToggleState();
        renderDepartmentMapSelection();
    });
    document.getElementById("startDate")?.addEventListener("input", () => {
        state.mapPreviewDepartment = null;
        renderFilterStatus();
        renderDepartmentMapSelection();
    });
    document.getElementById("endDate")?.addEventListener("input", () => {
        state.mapPreviewDepartment = null;
        renderFilterStatus();
        renderDepartmentMapSelection();
    });
    document.getElementById("estimateForm")?.addEventListener("submit", handleEstimateSubmit);
    document.getElementById("electricityImportForm")?.addEventListener("submit", handleElectricityImport);
    document.getElementById("weatherImportForm")?.addEventListener("submit", handleWeatherImport);
    document.querySelectorAll(".scenario-preset").forEach((button) => {
        button.addEventListener("click", () => applyScenarioPreset(button.dataset));
    });
    window.addEventListener("resize", debounce(() => {
        if (state.overview) {
            renderOverview();
        }
    }, 160));
}

async function loadDepartments() {
    const cachedDepartments = readSessionJson(STORAGE_KEYS.departments);
    if (Array.isArray(cachedDepartments) && cachedDepartments.length) {
        state.departments = cachedDepartments;
        renderDepartmentChecklist();
        return;
    }

    state.departments = await fetchJson("/api/dashboard/departments");
    writeSessionJson(STORAGE_KEYS.departments, state.departments);
    renderDepartmentChecklist();
}

function renderDepartmentChecklist() {
    const host = document.getElementById("departmentChecklist");
    if (!host) {
        return;
    }
    host.innerHTML = "";

    state.departments.forEach((department) => {
        const label = document.createElement("label");
        label.className = "department-pill";
        label.innerHTML = `
            <input type="checkbox" value="${department.code}" checked>
            <span>${department.code} - ${department.label}</span>
        `;
        host.appendChild(label);
    });
}

function getSelectedDepartments() {
    const checked = Array.from(document.querySelectorAll("#departmentChecklist input:checked"))
        .map((checkbox) => checkbox.value);
    return checked.length ? checked : state.departments.map((department) => department.code);
}

function setSelectedDepartments(departmentCodes) {
    const selectedDepartments = new Set(departmentCodes);
    document.querySelectorAll("#departmentChecklist input").forEach((checkbox) => {
        checkbox.checked = selectedDepartments.has(checkbox.value);
    });
    syncDepartmentToggleState();
}

function getDepartmentLabel(departmentCode) {
    return state.departments.find((department) => department.code === departmentCode)?.label || departmentCode;
}

function renderMapLabelMarkup(department) {
    const labelLines = department.labelLines?.length ? department.labelLines : [getDepartmentLabel(department.code)];
    const lineOffset = labelLines.length > 1 ? 10 : 0;

    return `
        <text class="dept-label" x="${department.labelX}" y="${department.labelY - lineOffset}">
            ${labelLines.map((line, index) => `
                <tspan x="${department.labelX}" dy="${index === 0 ? 0 : 18}">${line}</tspan>
            `).join("")}
        </text>
    `;
}

function overviewMatchesCurrentFilters() {
    if (!state.overview) {
        return false;
    }

    const currentFilters = buildFilterState();
    const appliedFilters = state.overview.filters;
    return currentFilters.startDate === appliedFilters.startDate
        && currentFilters.endDate === appliedFilters.endDate
        && currentFilters.departments.length === appliedFilters.selectedDepartments.length
        && currentFilters.departments.every((departmentCode, index) => departmentCode === appliedFilters.selectedDepartments[index]);
}

function getDisplayedSelectedDepartments() {
    if (state.mapPreviewDepartment) {
        return [state.mapPreviewDepartment];
    }
    if (overviewMatchesCurrentFilters()) {
        return state.overview.filters.selectedDepartments;
    }
    if (document.getElementById("departmentChecklist")) {
        return getSelectedDepartments();
    }
    return state.overview?.filters?.selectedDepartments || [];
}

function ensureFilterStatusBar() {
    if (document.getElementById("filterStatusBar")) {
        return;
    }

    const navigation = document.querySelector(".hero .section-nav");
    if (!navigation) {
        return;
    }

    const statusBar = document.createElement("div");
    statusBar.id = "filterStatusBar";
    statusBar.className = "filter-status-bar";
    navigation.insertAdjacentElement("afterend", statusBar);
}

function renderFilterStatus() {
    const host = document.getElementById("filterStatusBar");
    if (!host || !document.getElementById("departmentChecklist")) {
        return;
    }

    const filters = buildFilterState();
    const departmentLabels = filters.departments.map(getDepartmentLabel);
    const status = state.loading
        ? { label: "Mise a jour en cours", className: "pending" }
        : overviewMatchesCurrentFilters()
            ? { label: "Vue a jour", className: "ready" }
            : { label: "Filtres modifies", className: "warning" };
    const coverage = overviewMatchesCurrentFilters() ? state.overview?.coverage : null;

    host.innerHTML = `
        <div class="status-header">
            <strong>Filtre actif</strong>
            <span class="status-badge ${status.className}">${status.label}</span>
        </div>
        <div class="status-chip-row">
            <span class="status-chip"><strong>Periode</strong>${filters.startDate} -> ${filters.endDate}</span>
            <span class="status-chip"><strong>Departements</strong>${formatInteger(filters.departments.length)} | ${summarizeDepartmentLabels(departmentLabels)}</span>
            <span class="status-chip"><strong>Observations</strong>${coverage ? `${formatInteger(coverage.joinedHourlyObservations)} jointes` : "Actualiser pour recalculer"}</span>
        </div>
    `;
}

function summarizeDepartmentLabels(labels) {
    if (!labels.length) {
        return "Aucun";
    }
    if (labels.length <= 3) {
        return labels.join(", ");
    }
    return `${labels.slice(0, 3).join(", ")} +${labels.length - 3}`;
}

function setLoading(isLoading) {
    state.loading = isLoading;
    document.body.classList.toggle("is-loading", isLoading);
    toggleBusyControls(isLoading);
    renderFilterStatus();
}

function toggleBusyControls(isDisabled) {
    document.querySelectorAll(
        "#applyFilters, #resetFilters, #toggleDepartments, #resetMapFocus, #estimateForm button[type='submit'], .scenario-preset"
    ).forEach((element) => {
        element.disabled = isDisabled;
    });
}

async function refreshOverview(options = {}) {
    const { force = false, silent = false } = options;
    const filters = buildFilterState();
    saveAppliedFilters(filters);

    const cachedOverview = force ? null : readOverviewCache(filters);
    if (cachedOverview?.data) {
        state.overview = cachedOverview.data;
        renderOverview();
        return state.overview;
    }

    if (!silent) {
        showLoadingState();
    }

    try {
        const params = new URLSearchParams({
            startDate: filters.startDate,
            endDate: filters.endDate,
            departments: filters.departments.join(",")
        });

        state.overview = await fetchJson(`/api/dashboard/overview?${params.toString()}`);
        writeOverviewCache(filters, state.overview);
        state.mapPreviewDepartment = null;
        renderOverview();
        return state.overview;
    } catch (error) {
        const fallbackOverview = readOverviewCache(filters, true);
        if (fallbackOverview?.data) {
            state.overview = fallbackOverview.data;
            state.mapPreviewDepartment = null;
            renderOverview();
            showToast("Dernier resultat en cache affiche.", "error");
            return state.overview;
        }
        state.mapPreviewDepartment = null;
        renderDepartmentMapSelection();
        throw error;
    } finally {
        if (!silent) {
            setLoading(false);
        }
    }
}

function renderOverview() {
    if (!state.overview) {
        return;
    }

    renderHeroHighlights();
    renderCoverage();
    renderSummaryCards();
    renderTrendChart();
    renderTerritory();
    renderDepartmentProfiles();
    renderDepartmentChart();
    renderScatterChart();
    renderBucketChart();
    renderSeasonCards();
    renderEstimateState();
    renderTransparency();
    renderDepartmentMapSelection();
    setLoading(false);
    renderFilterStatus();
}

function renderHeroHighlights() {
    const host = document.getElementById("heroHighlights");
    if (!host) {
        return;
    }
    const coverage = state.overview.coverage;
    const filters = state.overview.filters;

    host.innerHTML = "";
    [
        {
            title: "Periode",
            body: coverage.periodLabel
        },
        {
            title: "Departements",
            body: filters.selectedDepartments.join(", ")
        },
        {
            title: "Observations communes",
            body: `${formatInteger(coverage.joinedHourlyObservations)} observations horaires`
        },
        {
            title: "Stations meteo",
            body: formatInteger(coverage.weatherStationCount)
        }
    ].forEach((item) => {
        const card = document.createElement("div");
        card.className = "hero-pill";
        card.innerHTML = `<strong>${item.title}</strong><span>${item.body}</span>`;
        host.appendChild(card);
    });
}

function renderCoverage() {
    const coverage = state.overview.coverage;
    const host = document.getElementById("coveragePanel");
    if (!host) {
        return;
    }
    host.innerHTML = `
        <div class="mini-stat"><strong>Periode</strong><div>${coverage.periodLabel}</div></div>
        <div class="mini-stat"><strong>Consommation</strong><div>${formatInteger(coverage.consumptionRows)} lignes</div></div>
        <div class="mini-stat"><strong>Meteo</strong><div>${formatInteger(coverage.weatherRows)} lignes</div></div>
        <div class="mini-stat"><strong>Stations</strong><div>${formatInteger(coverage.weatherStationCount)}</div></div>
    `;
}

function renderSummaryCards() {
    const summary = state.overview.summary;
    const host = document.getElementById("summaryCards");
    if (!host) {
        return;
    }

    const cards = [
        { label: "Correlation T / Conso", value: summary.correlationTemperatureConsumption, suffix: "", tone: "teal" },
        { label: "Consommation moyenne", value: summary.averageConsumptionMw, suffix: " MW", tone: "orange" },
        { label: "Temperature moyenne", value: summary.averageTemperature, suffix: " deg C", tone: "gold" },
        { label: "Pic de consommation", value: summary.peakConsumptionMw, suffix: " MW", tone: "orange" },
        { label: "Heures froides", value: summary.coldHoursAverageConsumption, suffix: " MW", tone: "teal" },
        { label: "Heures chaudes", value: summary.warmHoursAverageConsumption, suffix: " MW", tone: "gold" }
    ];

    host.innerHTML = cards.map((card) => `
        <div class="metric-card ${card.tone}">
            <span class="label">${card.label}</span>
            <span class="value">${formatNumber(card.value)}${card.suffix}</span>
        </div>
    `).join("");
}

function renderTrendChart() {
    const host = document.getElementById("trendChart");
    if (!host) {
        return;
    }
    renderNormalizedLineChart(
        host,
        state.overview.dailyTrends,
        [
            { key: "averageConsumptionMw", label: "Consommation", color: "#c8613d" },
            { key: "averageTemperature", label: "Temperature", color: "#1d7f7a" },
            { key: "averageHumidity", label: "Humidite", color: "#3d65c8" },
            { key: "averageWind", label: "Vent", color: "#d3a24f" }
        ],
        { mode: "normalized" }
    );
}

function renderTerritory() {
    const coverage = state.overview.coverage;
    const filters = state.overview.filters;
    const host = document.getElementById("territoryOverview");
    if (!host) {
        return;
    }

    host.innerHTML = `
        <div class="territory-card">
            <span class="label">Departements actifs</span>
            <span class="value">${filters.selectedDepartments.join(", ")}</span>
        </div>
        <div class="territory-card">
            <span class="label">Regions importees</span>
            <span class="value">${coverage.importedConsumptionRegions.join(", ") || "Aucune"}</span>
        </div>
        <div class="territory-card">
            <span class="label">Observations jointes</span>
            <span class="value">${formatInteger(coverage.joinedHourlyObservations)}</span>
        </div>
        <div class="territory-card">
            <span class="label">Stations meteo</span>
            <span class="value">${formatInteger(coverage.weatherStationCount)}</span>
        </div>
    `;
}

function renderDepartmentProfiles() {
    const host = document.getElementById("departmentProfiles");
    if (!host) {
        return;
    }
    const profiles = state.overview.departmentProfiles;

    if (!profiles.length) {
        renderEmpty(host, "Aucune donnee meteo disponible pour les departements selectionnes.");
        return;
    }

    host.innerHTML = profiles.map((profile) => `
        <div class="profile-card">
            <span class="label">${profile.departement} - ${profile.label}</span>
            <div class="subcopy">Temperature moyenne: <strong>${formatNumber(profile.averageTemperature)} deg C</strong></div>
            <div class="subcopy">Humidite moyenne: <strong>${formatNumber(profile.averageHumidity)} %</strong></div>
            <div class="subcopy">Vent moyen: <strong>${formatNumber(profile.averageWind)}</strong></div>
            <div class="subcopy">Amplitude thermique: <strong>${formatNumber(profile.minimumTemperature)} a ${formatNumber(profile.maximumTemperature)} deg C</strong></div>
            <div class="subcopy">Stations: <strong>${formatInteger(profile.stationCount)}</strong></div>
        </div>
    `).join("");
}

function renderDepartmentChart() {
    const host = document.getElementById("departmentChart");
    if (!host) {
        return;
    }
    renderDepartmentLineChart(host, state.overview.departmentMonthlyTrends);
}

function renderScatterChart() {
    const host = document.getElementById("scatterChart");
    if (!host) {
        return;
    }
    renderScatter(host, state.overview.scatterPoints);
}

function renderBucketChart() {
    const host = document.getElementById("bucketChart");
    if (!host) {
        return;
    }
    renderBarChart(
        host,
        state.overview.temperatureBuckets,
        (item) => `${formatNumber(item.bucketStart)} a ${formatNumber(item.bucketEnd)} deg`,
        (item) => item.averageConsumptionMw,
        (item) => "#c8613d"
    );
}

function renderGrandEstMap() {
    const host = document.getElementById("grandEstMap");
    if (!host) {
        return;
    }

    host.innerHTML = `
        ${GRAND_EST_MAP_DEPARTMENTS.map((department) => `
            <g class="map-department ${department.tone}" data-dept="${department.code}" tabindex="0" role="button" aria-label="${getDepartmentLabel(department.code)} (${department.code})">
                <path class="dept-shape" d="${department.path}"></path>
                ${renderMapLabelMarkup(department)}
            </g>
        `).join("")}
    `;

    host.querySelectorAll(".map-department").forEach((departmentNode) => {
        departmentNode.addEventListener("click", () => {
            focusDepartmentFromMap(departmentNode.dataset.dept);
        });
        departmentNode.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                focusDepartmentFromMap(departmentNode.dataset.dept);
            }
        });
    });
}

async function focusDepartmentFromMap(departmentCode) {
    if (!departmentCode) {
        return;
    }

    state.mapPreviewDepartment = departmentCode;
    setSelectedDepartments([departmentCode]);
    renderDepartmentMapSelection();
    await refreshOverview({ force: true });
}

async function focusAllDepartmentsFromMap() {
    if (!state.departments.length) {
        return;
    }

    state.mapPreviewDepartment = null;
    setSelectedDepartments(state.departments.map((department) => department.code));
    await refreshOverview({ force: true });
}

function renderDepartmentMapSelection() {
    const selectedDepartments = getDisplayedSelectedDepartments();
    const selectedDepartmentSet = new Set(selectedDepartments);
    document.querySelectorAll("#grandEstMap .map-department").forEach((departmentNode) => {
        const isSelected = selectedDepartmentSet.has(departmentNode.dataset.dept);
        departmentNode.classList.toggle("is-selected", isSelected);
        departmentNode.setAttribute("aria-pressed", String(isSelected));
    });

    const note = document.getElementById("mapSelectionNote");
    if (note) {
        if (state.mapPreviewDepartment) {
            note.textContent = `Filtrage en cours sur ${getDepartmentLabel(state.mapPreviewDepartment)}.`;
        } else if (selectedDepartments.length === 1) {
            note.textContent = `Departement actif: ${getDepartmentLabel(selectedDepartments[0])}. Les indicateurs de la page sont filtres sur ce territoire.`;
        } else if (selectedDepartments.length) {
            note.textContent = `Departements visibles: ${selectedDepartments.map(getDepartmentLabel).join(", ")}. Clique sur la carte pour focaliser directement un departement.`;
        } else {
            note.textContent = "Clique sur un departement pour appliquer directement le filtre sur cette vue.";
        }
    }

    renderDepartmentSpotlight(selectedDepartments);
}

function renderDepartmentSpotlight(selectedDepartments = getDisplayedSelectedDepartments()) {
    const host = document.getElementById("departmentSpotlight");
    if (!host) {
        return;
    }

    if (!selectedDepartments.length) {
        host.innerHTML = `
            <div class="map-detail-placeholder">
                <span class="map-badge">Selection carte</span>
                <h3>Aucun departement cible</h3>
                <p class="subcopy">Clique sur un departement de la carte pour afficher son profil et filtrer automatiquement cette page.</p>
            </div>
        `;
        return;
    }

    if (selectedDepartments.length > 1) {
        host.innerHTML = `
            <span class="map-badge">Vue regionale</span>
            <div class="map-detail-header">
                <h3>Selection multiple</h3>
                <p class="subcopy">La page affiche actuellement ${formatInteger(selectedDepartments.length)} departements: ${selectedDepartments.map(getDepartmentLabel).join(", ")}.</p>
            </div>
            <div class="map-detail-grid">
                <div class="map-detail-stat">
                    <span>Departements actifs</span>
                    <strong>${formatInteger(selectedDepartments.length)}</strong>
                </div>
                <div class="map-detail-stat">
                    <span>Mode de lecture</span>
                    <strong>Vue comparee</strong>
                </div>
            </div>
            <p class="subcopy">Clique sur un seul departement de la carte pour isoler son profil et rafraichir directement les indicateurs.</p>
        `;
        return;
    }

    const departmentCode = selectedDepartments[0];
    const profile = state.overview?.departmentProfiles?.find((item) => item.departement === departmentCode);
    const isPending = state.mapPreviewDepartment != null || !overviewMatchesCurrentFilters();
    if (!profile) {
        host.innerHTML = `
            <span class="map-badge ${isPending ? "pending" : ""}">${isPending ? "Mise a jour" : "Selection carte"}</span>
            <div class="map-detail-header">
                <h3>${getDepartmentLabel(departmentCode)}</h3>
                <p class="subcopy">Les details de ce departement sont en cours de chargement.</p>
            </div>
        `;
        return;
    }

    host.innerHTML = `
        <span class="map-badge ${isPending ? "pending" : ""}">${isPending ? "Mise a jour en cours" : "Departement cible"}</span>
        <div class="map-detail-header">
            <h3>${profile.label}</h3>
            <p class="subcopy">Code ${profile.departement} | Periode ${state.overview?.coverage?.periodLabel || "-"}</p>
        </div>
        <div class="map-detail-grid">
            <div class="map-detail-stat">
                <span>Temperature moyenne</span>
                <strong>${formatNumber(profile.averageTemperature)} deg C</strong>
            </div>
            <div class="map-detail-stat">
                <span>Humidite moyenne</span>
                <strong>${formatNumber(profile.averageHumidity)} %</strong>
            </div>
            <div class="map-detail-stat">
                <span>Vent moyen</span>
                <strong>${formatNumber(profile.averageWind)}</strong>
            </div>
            <div class="map-detail-stat">
                <span>Amplitude thermique</span>
                <strong>${formatNumber(profile.minimumTemperature)} a ${formatNumber(profile.maximumTemperature)} deg C</strong>
            </div>
            <div class="map-detail-stat">
                <span>Stations</span>
                <strong>${formatInteger(profile.stationCount)}</strong>
            </div>
            <div class="map-detail-stat">
                <span>Observations</span>
                <strong>${formatInteger(profile.observationCount)}</strong>
            </div>
        </div>
        <div class="map-detail-footer">
            <p class="subcopy">Le detail meteo affiche ici provient des observations du departement selectionne.</p>
            <a href="/explorer.html" class="map-detail-link">Voir le profil detaille</a>
        </div>
    `;
}

function renderSeasonCards() {
    const host = document.getElementById("seasonCards");
    if (!host) {
        return;
    }
    const seasons = state.overview.seasonalComparisons;

    if (!seasons.length) {
        renderEmpty(host, "Aucune vue saisonniere disponible.");
        return;
    }

    host.innerHTML = seasons.map((season) => `
        <div class="season-card">
            <span class="label">${season.label}</span>
            <span class="value">${formatNumber(season.averageConsumptionMw)} MW</span>
            <div class="subcopy">Temperature: ${formatNumber(season.averageTemperature)} deg C</div>
            <div class="subcopy">Correlation: ${formatNumber(season.correlation)}</div>
            <div class="subcopy">Observations: ${formatInteger(season.observationCount)}</div>
        </div>
    `).join("");
}

function renderEstimateState() {
    const host = document.getElementById("estimateResult");
    if (!host) {
        return;
    }
    const model = state.overview.regressionModel;

    if (!model.ready) {
        host.innerHTML = `
            <h3>Modele indisponible</h3>
            <p class="subcopy">Importez davantage de donnees completes pour activer l'estimation.</p>
        `;
        return;
    }

    applyScenarioDefaults(model.defaultInputs);
    host.innerHTML = `
        <h3>Modele actif</h3>
        <div class="estimate-figure">R2 = ${formatNumber(model.rSquared)}</div>
        <p class="subcopy">${model.label}</p>
        <p class="subcopy">Observations utilisees: ${formatInteger(model.observationsUsed)}</p>
    `;
}

function renderTransparency() {
    const transparency = state.overview.transparency;
    const sources = document.getElementById("sourcesList");
    const steps = document.getElementById("stepsList");
    const notes = document.getElementById("notesList");
    if (!sources || !steps || !notes) {
        return;
    }
    sources.innerHTML = transparency.sources.map((item) => `<li>${item}</li>`).join("");
    steps.innerHTML = transparency.processingSteps.map((item) => `<li>${item}</li>`).join("");
    notes.innerHTML = transparency.notes.map((item) => `<li>${item}</li>`).join("");
}

async function handleEstimateSubmit(event) {
    event.preventDefault();
    if (!document.getElementById("scenarioTemperature")) {
        return;
    }

    const host = document.getElementById("estimateResult");
    const submitButton = event.submitter || event.currentTarget.querySelector("button[type='submit']");
    const initialLabel = submitButton ? submitButton.textContent : "";
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = "Calcul en cours...";
    }
    if (host) {
        renderEstimateLoadingState(host);
    }

    const payload = {
        departments: getSelectedDepartments(),
        startDate: document.getElementById("startDate").value,
        endDate: document.getElementById("endDate").value,
        temperature: parseMaybeNumber(document.getElementById("scenarioTemperature").value),
        humidity: parseMaybeNumber(document.getElementById("scenarioHumidity").value),
        wind: parseMaybeNumber(document.getElementById("scenarioWind").value),
        precipitations: parseMaybeNumber(document.getElementById("scenarioRain").value)
    };

    try {
        const response = await fetchJson("/api/dashboard/estimate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.modelReady) {
            host.innerHTML = `<h3>Estimation impossible</h3><p class="subcopy">${response.message}</p>`;
            return;
        }

        host.innerHTML = `
            <h3>Consommation estimee</h3>
            <div class="estimate-figure">${formatNumber(response.estimatedConsumptionMw)} MW</div>
            <p class="subcopy">${response.message}</p>
            <p class="subcopy">R2 du modele: ${formatNumber(response.rSquared)}</p>
            <p class="subcopy">Variables utilisees: ${Object.entries(response.inputsUsed)
                .map(([key, value]) => `${key}=${formatNumber(value)}`)
                .join(" | ")}</p>
        `;
    } catch (error) {
        host.innerHTML = `
            <h3>Estimation indisponible</h3>
            <p class="subcopy">${error.message || "Le calcul n'a pas pu aboutir."}</p>
        `;
        showToast("Impossible de calculer l'estimation.", "error");
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = initialLabel;
        }
    }
}

async function handleElectricityImport(event) {
    event.preventDefault();
    const input = document.getElementById("electricityFile");
    if (!input) {
        return;
    }
    if (!input.files.length) {
        showToast("Selectionne un fichier RTE.", "error");
        return;
    }

    const formData = new FormData();
    formData.append("file", input.files[0]);
    const message = await fetchText("/api/donnees/importer-electricite", {
        method: "POST",
        body: formData
    });

    document.getElementById("importStatus").textContent = message;
    showToast(message, "success");
    input.value = "";
    await refreshOverview();
}

async function handleWeatherImport(event) {
    event.preventDefault();
    const input = document.getElementById("weatherFiles");
    if (!input) {
        return;
    }
    if (!input.files.length) {
        showToast("Selectionne au moins un fichier meteo.", "error");
        return;
    }

    const formData = new FormData();
    Array.from(input.files).forEach((file) => formData.append("files", file));
    const message = await fetchText("/api/donnees/importer-meteo", {
        method: "POST",
        body: formData
    });

    document.getElementById("importStatus").textContent = message;
    showToast(message, "success");
    input.value = "";
    await refreshOverview();
}

function resetFilters() {
    if (!document.getElementById("startDate")) {
        return;
    }
    state.mapPreviewDepartment = null;
    document.getElementById("startDate").value = DEFAULT_START_DATE;
    document.getElementById("endDate").value = DEFAULT_END_DATE;
    document.querySelectorAll("#departmentChecklist input").forEach((checkbox) => {
        checkbox.checked = true;
    });
    state.deselected = false;
    syncDepartmentToggleState();
    refreshOverview({ force: true });
}

function toggleDepartments() {
    if (!document.getElementById("toggleDepartments")) {
        return;
    }
    state.deselected = !state.deselected;
    document.querySelectorAll("#departmentChecklist input").forEach((checkbox) => {
        checkbox.checked = !state.deselected;
    });
    syncDepartmentToggleState();
}

function applyScenarioDefaults(defaultInputs) {
    if (!defaultInputs) {
        return;
    }

    if (document.getElementById("scenarioTemperature") && !document.getElementById("scenarioTemperature").value && defaultInputs.temperature != null) {
        document.getElementById("scenarioTemperature").value = defaultInputs.temperature;
    }
    if (document.getElementById("scenarioHumidity") && !document.getElementById("scenarioHumidity").value && defaultInputs.humidity != null) {
        document.getElementById("scenarioHumidity").value = defaultInputs.humidity;
    }
    if (document.getElementById("scenarioWind") && !document.getElementById("scenarioWind").value && defaultInputs.wind != null) {
        document.getElementById("scenarioWind").value = defaultInputs.wind;
    }
    if (document.getElementById("scenarioRain") && !document.getElementById("scenarioRain").value && defaultInputs.precipitations != null) {
        document.getElementById("scenarioRain").value = defaultInputs.precipitations;
    }
}

function applyScenarioPreset(dataset) {
    if (!document.getElementById("scenarioTemperature")) {
        return;
    }

    if (dataset.temperature != null) {
        document.getElementById("scenarioTemperature").value = dataset.temperature;
    }
    if (dataset.humidity != null) {
        document.getElementById("scenarioHumidity").value = dataset.humidity;
    }
    if (dataset.wind != null) {
        document.getElementById("scenarioWind").value = dataset.wind;
    }
    if (dataset.rain != null) {
        document.getElementById("scenarioRain").value = dataset.rain;
    }
}

function buildFilterState() {
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    return {
        startDate: startDateInput ? startDateInput.value || DEFAULT_START_DATE : DEFAULT_START_DATE,
        endDate: endDateInput ? endDateInput.value || DEFAULT_END_DATE : DEFAULT_END_DATE,
        departments: getSelectedDepartments()
    };
}

function restoreAppliedFilters() {
    state.mapPreviewDepartment = null;
    const filters = normalizeStoredFilters(readSessionJson(STORAGE_KEYS.filters));
    const startDateInput = document.getElementById("startDate");
    const endDateInput = document.getElementById("endDate");

    if (startDateInput) {
        startDateInput.value = filters.startDate;
    }
    if (endDateInput) {
        endDateInput.value = filters.endDate;
    }

    const selectedDepartments = new Set(filters.departments);
    document.querySelectorAll("#departmentChecklist input").forEach((checkbox) => {
        checkbox.checked = selectedDepartments.has(checkbox.value);
    });
    syncDepartmentToggleState();
}

function saveAppliedFilters(filters) {
    writeSessionJson(STORAGE_KEYS.filters, filters);
}

function normalizeStoredFilters(storedFilters) {
    return {
        startDate: storedFilters?.startDate || DEFAULT_START_DATE,
        endDate: storedFilters?.endDate || DEFAULT_END_DATE,
        departments: normalizeStoredDepartments(storedFilters?.departments)
    };
}

function normalizeStoredDepartments(departments) {
    const validDepartmentCodes = new Set(state.departments.map((department) => department.code));
    const selectedDepartments = Array.isArray(departments)
        ? departments.filter((departmentCode) => validDepartmentCodes.has(departmentCode))
        : [];

    return selectedDepartments.length
        ? selectedDepartments
        : state.departments.map((department) => department.code);
}

function syncDepartmentToggleState() {
    const toggleButton = document.getElementById("toggleDepartments");
    if (!toggleButton) {
        return;
    }

    const checkboxes = Array.from(document.querySelectorAll("#departmentChecklist input"));
    const selectedCount = checkboxes.filter((checkbox) => checkbox.checked).length;
    state.deselected = selectedCount === 0;
    toggleButton.textContent = state.deselected ? "Tout selectionner" : "Tout deselectionner";
    renderFilterStatus();
}

function showLoadingState() {
    setLoading(true);
    const coveragePanel = document.getElementById("coveragePanel");
    if (coveragePanel) {
        coveragePanel.innerHTML = '<div class="mini-stat">Chargement en cours...</div>';
    }
    renderMetricLoadingState(document.getElementById("summaryCards"));
    renderChartLoadingState(document.getElementById("trendChart"));
    renderChartLoadingState(document.getElementById("departmentChart"));
    renderChartLoadingState(document.getElementById("scatterChart"));
    renderChartLoadingState(document.getElementById("bucketChart"));
    renderGridLoadingState(document.getElementById("territoryOverview"), 4);
    renderGridLoadingState(document.getElementById("departmentProfiles"), 3);
    renderGridLoadingState(document.getElementById("seasonCards"), 4);
    renderEstimateLoadingState(document.getElementById("estimateResult"));
    renderMapDetailLoadingState(document.getElementById("departmentSpotlight"));
    renderListLoadingState(document.getElementById("sourcesList"));
    renderListLoadingState(document.getElementById("stepsList"));
    renderListLoadingState(document.getElementById("notesList"));
}

function renderMetricLoadingState(host) {
    if (!host) {
        return;
    }
    host.innerHTML = Array.from({ length: 4 }, () => `
        <div class="metric-card loading-card">
            <span class="skeleton-line short"></span>
            <span class="skeleton-line large"></span>
        </div>
    `).join("");
}

function renderGridLoadingState(host, count) {
    if (!host) {
        return;
    }
    host.innerHTML = Array.from({ length: count }, () => `
        <div class="loading-panel-card">
            <span class="skeleton-line medium"></span>
            <span class="skeleton-line short"></span>
            <span class="skeleton-line short"></span>
        </div>
    `).join("");
}

function renderChartLoadingState(host) {
    if (!host) {
        return;
    }
    host.innerHTML = `
        <div class="chart-loading-state">
            <div class="skeleton-graph"></div>
            <div class="skeleton-line medium"></div>
        </div>
    `;
}

function renderEstimateLoadingState(host) {
    if (!host) {
        return;
    }
    host.innerHTML = `
        <div class="loading-panel-card">
            <span class="skeleton-line short"></span>
            <span class="skeleton-line large"></span>
            <span class="skeleton-line medium"></span>
        </div>
    `;
}

function renderMapDetailLoadingState(host) {
    if (!host) {
        return;
    }
    host.innerHTML = `
        <span class="map-badge pending">Mise a jour</span>
        <div class="loading-panel-card">
            <span class="skeleton-line medium"></span>
            <span class="skeleton-line short"></span>
            <span class="skeleton-line short"></span>
        </div>
    `;
}

function renderListLoadingState(host) {
    if (!host) {
        return;
    }
    host.innerHTML = Array.from({ length: 4 }, () => `
        <li class="loading-list-item"><span class="skeleton-line medium"></span></li>
    `).join("");
}

function getOverviewCacheKey(filters) {
    return STORAGE_KEYS.overviewPrefix + [
        filters.startDate,
        filters.endDate,
        filters.departments.join(",")
    ].join("|");
}

function readOverviewCache(filters, allowStale = false) {
    const payload = readSessionJson(getOverviewCacheKey(filters));
    if (!payload?.data || payload.savedAt == null) {
        return null;
    }

    const stale = Date.now() - payload.savedAt > OVERVIEW_CACHE_TTL_MS;
    if (stale && !allowStale) {
        return null;
    }

    return { data: payload.data, stale };
}

function writeOverviewCache(filters, overview) {
    writeSessionJson(getOverviewCacheKey(filters), {
        savedAt: Date.now(),
        data: overview
    });
}

function readSessionJson(key) {
    try {
        const rawValue = sessionStorage.getItem(key);
        return rawValue ? JSON.parse(rawValue) : null;
    } catch (error) {
        console.warn("Impossible de lire le cache navigateur.", error);
        return null;
    }
}

function writeSessionJson(key, value) {
    try {
        sessionStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
        console.warn("Impossible d'ecrire dans le cache navigateur.", error);
    }
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Erreur API");
    }
    return response.json();
}

async function fetchText(url, options = {}) {
    const response = await fetch(url, options);
    const message = await response.text();
    if (!response.ok) {
        showToast(message || "Une erreur est survenue.", "error");
        throw new Error(message || "Erreur API");
    }
    return message;
}

function renderNormalizedLineChart(container, points, seriesDefinitions, options = {}) {
    if (!points.length) {
        renderEmpty(container, "Aucune serie disponible pour cette visualisation.");
        return;
    }

    const width = 840;
    const height = 320;
    const margin = { top: 20, right: 24, bottom: 42, left: 36 };
    const usableWidth = width - margin.left - margin.right;
    const usableHeight = height - margin.top - margin.bottom;

    const svg = svgElement("svg", { viewBox: `0 0 ${width} ${height}` });
    svg.appendChild(renderGrid(width, height, margin, 5));

    const legend = document.createElement("div");
    legend.className = "legend";

    seriesDefinitions.forEach((series) => {
        const values = points
            .map((point) => point[series.key])
            .filter((value) => value !== null && value !== undefined);

        if (!values.length) {
            return;
        }

        const min = Math.min(...values);
        const max = Math.max(...values);
        const pathData = points.map((point, index) => {
            const rawValue = point[series.key];
            const normalized = rawValue == null
                ? null
                : (max === min ? 0.5 : (rawValue - min) / (max - min));

            return {
                x: margin.left + (index / Math.max(points.length - 1, 1)) * usableWidth,
                y: normalized == null ? null : margin.top + (1 - normalized) * usableHeight
            };
        });

        const path = createPath(pathData);
        if (path) {
            svg.appendChild(svgElement("path", {
                d: path,
                fill: "none",
                stroke: series.color,
                "stroke-width": 2.8,
                "stroke-linecap": "round",
                "stroke-linejoin": "round"
            }));
        }

        legend.appendChild(createLegendItem(series.label, series.color));
    });

    appendAxisLabels(svg, points, width, height, margin);
    container.innerHTML = "";
    container.appendChild(svg);

    if (options.mode === "normalized") {
        const note = document.createElement("p");
        note.className = "subcopy";
        note.textContent = "Lecture normalisee: chaque courbe est remise sur sa propre echelle pour comparer les formes.";
        container.appendChild(note);
    }
    container.appendChild(legend);
}

function renderDepartmentLineChart(container, points) {
    if (!points.length) {
        renderEmpty(container, "Aucune courbe mensuelle disponible.");
        return;
    }

    const width = 840;
    const height = 320;
    const margin = { top: 24, right: 24, bottom: 42, left: 42 };
    const usableWidth = width - margin.left - margin.right;
    const usableHeight = height - margin.top - margin.bottom;
    const colors = ["#c8613d", "#1d7f7a", "#d3a24f", "#3d65c8", "#8c4fc2", "#2b3f58", "#b34772", "#448b53"];

    const grouped = groupBy(points, "departement");
    const allValues = points.map((point) => point.averageTemperature);
    const min = Math.min(...allValues);
    const max = Math.max(...allValues);

    const svg = svgElement("svg", { viewBox: `0 0 ${width} ${height}` });
    svg.appendChild(renderGrid(width, height, margin, 5));

    Object.entries(grouped).forEach(([department, departmentPoints], index) => {
        const sorted = departmentPoints.sort((a, b) => a.month - b.month);
        const path = createPath(sorted.map((point) => ({
            x: margin.left + ((point.month - 1) / 11) * usableWidth,
            y: margin.top + (1 - normalize(point.averageTemperature, min, max)) * usableHeight
        })));

        if (path) {
            svg.appendChild(svgElement("path", {
                d: path,
                fill: "none",
                stroke: colors[index % colors.length],
                "stroke-width": 2.4
            }));
        }
    });

    const monthLabels = ["Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Aou", "Sep", "Oct", "Nov", "Dec"];
    monthLabels.forEach((label, index) => {
        svg.appendChild(svgElement("text", {
            x: margin.left + (index / 11) * usableWidth,
            y: height - 10,
            fill: "#5f6a73",
            "font-size": 11,
            "text-anchor": "middle"
        }, label));
    });

    const legend = document.createElement("div");
    legend.className = "legend";
    Object.entries(grouped).forEach(([department, departmentPoints], index) => {
        legend.appendChild(createLegendItem(
            `${department} - ${departmentPoints[0].label}`,
            colors[index % colors.length]
        ));
    });

    container.innerHTML = "";
    container.appendChild(svg);
    container.appendChild(legend);
}

function renderScatter(container, points) {
    if (!points.length) {
        renderEmpty(container, "Aucun nuage de points disponible.");
        return;
    }

    const sampleStep = Math.max(1, Math.ceil(points.length / 1800));
    const sample = points.filter((_, index) => index % sampleStep === 0);
    const width = 800;
    const height = 320;
    const margin = { top: 20, right: 20, bottom: 42, left: 46 };
    const usableWidth = width - margin.left - margin.right;
    const usableHeight = height - margin.top - margin.bottom;
    const temperatures = sample.map((point) => point.temperature);
    const consumptions = sample.map((point) => point.consumptionMw);
    const minX = Math.min(...temperatures);
    const maxX = Math.max(...temperatures);
    const minY = Math.min(...consumptions);
    const maxY = Math.max(...consumptions);
    const colors = { Hiver: "#1d7f7a", Printemps: "#3d65c8", Ete: "#c8613d", Automne: "#d3a24f" };

    const svg = svgElement("svg", { viewBox: `0 0 ${width} ${height}` });
    svg.appendChild(renderGrid(width, height, margin, 5));

    sample.forEach((point) => {
        svg.appendChild(svgElement("circle", {
            cx: margin.left + normalize(point.temperature, minX, maxX) * usableWidth,
            cy: margin.top + (1 - normalize(point.consumptionMw, minY, maxY)) * usableHeight,
            r: 3,
            fill: colors[point.season] || "#5f6a73",
            opacity: 0.72
        }));
    });

    svg.appendChild(svgElement("text", {
        x: width / 2,
        y: height - 10,
        fill: "#5f6a73",
        "font-size": 12,
        "text-anchor": "middle"
    }, "Temperature"));
    svg.appendChild(svgElement("text", {
        x: 16,
        y: height / 2,
        fill: "#5f6a73",
        "font-size": 12,
        transform: `rotate(-90 16 ${height / 2})`,
        "text-anchor": "middle"
    }, "Consommation"));

    const legend = document.createElement("div");
    legend.className = "legend";
    Object.entries(colors).forEach(([label, color]) => legend.appendChild(createLegendItem(label, color)));

    container.innerHTML = "";
    container.appendChild(svg);
    container.appendChild(legend);
}

function renderBarChart(container, points, labelAccessor, valueAccessor, colorAccessor) {
    if (!points.length) {
        renderEmpty(container, "Aucune serie disponible.");
        return;
    }

    const width = 840;
    const height = 320;
    const margin = { top: 20, right: 20, bottom: 74, left: 42 };
    const usableWidth = width - margin.left - margin.right;
    const usableHeight = height - margin.top - margin.bottom;
    const values = points.map(valueAccessor);
    const max = Math.max(...values, 0);
    const barWidth = usableWidth / points.length;

    const svg = svgElement("svg", { viewBox: `0 0 ${width} ${height}` });
    svg.appendChild(renderGrid(width, height, margin, 5));

    points.forEach((point, index) => {
        const value = valueAccessor(point);
        const barHeight = max === 0 ? 0 : (value / max) * usableHeight;
        const x = margin.left + index * barWidth + 6;
        const y = margin.top + usableHeight - barHeight;
        svg.appendChild(svgElement("rect", {
            x,
            y,
            width: Math.max(barWidth - 12, 10),
            height: barHeight,
            rx: 8,
            fill: colorAccessor(point)
        }));
        svg.appendChild(svgElement("text", {
            x: x + Math.max(barWidth - 12, 10) / 2,
            y: height - 24,
            fill: "#5f6a73",
            "font-size": 10,
            "text-anchor": "middle"
        }, labelAccessor(point)));
    });

    container.innerHTML = "";
    container.appendChild(svg);
}

function renderGrid(width, height, margin, steps) {
    const group = svgElement("g");
    for (let index = 0; index <= steps; index += 1) {
        const y = margin.top + ((height - margin.top - margin.bottom) / steps) * index;
        group.appendChild(svgElement("line", {
            x1: margin.left,
            y1: y,
            x2: width - margin.right,
            y2: y,
            stroke: "rgba(31, 36, 48, 0.08)"
        }));
    }
    return group;
}

function appendAxisLabels(svg, points, width, height, margin) {
    const indices = [0, Math.floor(points.length / 4), Math.floor(points.length / 2), Math.floor((points.length * 3) / 4), points.length - 1]
        .filter((value, index, array) => array.indexOf(value) === index);

    indices.forEach((index) => {
        const point = points[index];
        svg.appendChild(svgElement("text", {
            x: margin.left + (index / Math.max(points.length - 1, 1)) * (width - margin.left - margin.right),
            y: height - 10,
            fill: "#5f6a73",
            "font-size": 10,
            "text-anchor": "middle"
        }, point.date));
    });
}

function createPath(points) {
    const validPoints = points.filter((point) => point.y !== null);
    if (validPoints.length < 2) {
        return "";
    }
    return validPoints.map((point, index) => `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`).join(" ");
}

function createLegendItem(label, color) {
    const item = document.createElement("span");
    item.className = "legend-item";
    item.innerHTML = `<span class="legend-swatch" style="background:${color}"></span>${label}`;
    return item;
}

function svgElement(tag, attributes = {}, textContent = "") {
    const node = document.createElementNS("http://www.w3.org/2000/svg", tag);
    Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, value));
    if (textContent) {
        node.textContent = textContent;
    }
    return node;
}

function renderEmpty(container, message) {
    container.innerHTML = `<div class="empty-state">${message}</div>`;
}

function normalize(value, min, max) {
    if (max === min) {
        return 0.5;
    }
    return (value - min) / (max - min);
}

function groupBy(items, key) {
    return items.reduce((accumulator, item) => {
        const value = item[key];
        accumulator[value] ||= [];
        accumulator[value].push(item);
        return accumulator;
    }, {});
}

function parseMaybeNumber(value) {
    if (value === "" || value == null) {
        return null;
    }
    return Number(value);
}

function formatNumber(value) {
    if (value == null || Number.isNaN(value)) {
        return "-";
    }
    return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 2 }).format(value);
}

function formatInteger(value) {
    if (value == null || Number.isNaN(value)) {
        return "-";
    }
    return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 }).format(value);
}

function showToast(message, type = "success") {
    const toast = document.getElementById("toast");
    if (!toast) {
        return;
    }
    toast.hidden = false;
    toast.className = `toast ${type}`;
    toast.textContent = message;
    clearTimeout(showToast.timeout);
    showToast.timeout = setTimeout(() => {
        toast.hidden = true;
    }, 4200);
}

function debounce(callback, delay) {
    let timeoutId;
    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => callback(...args), delay);
    };
}
