// ============================================
// ÉlectroMétéo Grand Est - JavaScript
// Interactive functionality
// Version corrigée pour correspondre au CSS
// ============================================

// === Global State ===
let currentFilters = {
    departments: ['08', '51', '54', '57'],
    dateStart: '2014-01-01',
    dateEnd: '2014-12-31',
    season: 'all',
    dayType: 'all'
};

// === Department Map Interactions ===
document.addEventListener('DOMContentLoaded', function() {
    console.log('ÉlectroMétéo Grand Est - Application chargée');
    
    // Initialize department map if present
    const departments = document.querySelectorAll('.department-area');
    const deptInfo = document.getElementById('dept-info');
    
    if (departments.length > 0) {
        console.log(`Found ${departments.length} department areas`);
        
        departments.forEach(dept => {
            dept.addEventListener('click', function() {
                const deptNumber = this.getAttribute('data-dept');
                const deptName = this.getAttribute('data-name');
                showDepartmentInfo(deptNumber, deptName);
            });
            
            dept.addEventListener('mouseenter', function() {
                this.style.opacity = '1';
                this.style.filter = 'brightness(1.2)';
            });
            
            dept.addEventListener('mouseleave', function() {
                this.style.opacity = '0.7';
                this.style.filter = 'brightness(1)';
            });
        });
    }
    
    // Initialize temperature slider if present
    const tempSlider = document.getElementById('temp-input');
    const tempDisplay = document.getElementById('temp-display');
    
    if (tempSlider && tempDisplay) {
        tempSlider.addEventListener('input', function() {
            tempDisplay.textContent = this.value;
            updateSliderColor(this);
        });
        
        // Initial color
        updateSliderColor(tempSlider);
    }
    
    // Initialize season buttons
    const seasonButtons = document.querySelectorAll('.season-option-btn, .season-btn');
    seasonButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            seasonButtons.forEach(b => b.classList.remove('active-season', 'active'));
            this.classList.add('active-season', 'active');
        });
    });
    
    // Initialize day type buttons
    const dayTypeButtons = document.querySelectorAll('.daytype-option-btn, .day-type-btn');
    dayTypeButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            dayTypeButtons.forEach(b => b.classList.remove('active-daytype', 'active'));
            this.classList.add('active-daytype', 'active');
        });
    });
    
    // Initialize chart type buttons
    const chartTypeButtons = document.querySelectorAll('.chart-type-btn');
    chartTypeButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            chartTypeButtons.forEach(b => b.classList.remove('active-chart', 'active'));
            this.classList.add('active-chart', 'active');
        });
    });
    
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
    
    // Update filter summary on page load
    updateFilterSummary();
});

// === Department Info Display ===
function showDepartmentInfo(deptNumber, deptName) {
    const infoCard = document.getElementById('dept-info');
    if (!infoCard) return;
    
    // Sample data - replace with real API call
    const deptData = {
        '08': { pop: '273K', conso: '850 MW', sensitivity: 'Moyenne' },
        '10': { pop: '310K', conso: '750 MW', sensitivity: 'Faible' },
        '51': { pop: '568K', conso: '900 MW', sensitivity: 'Faible' },
        '52': { pop: '175K', conso: '650 MW', sensitivity: 'Moyenne' },
        '54': { pop: '733K', conso: '950 MW', sensitivity: 'Moyenne' },
        '55': { pop: '187K', conso: '600 MW', sensitivity: 'Moyenne' },
        '57': { pop: '1M', conso: '1000 MW', sensitivity: 'Moyenne' },
        '67': { pop: '1.1M', conso: '920 MW', sensitivity: 'Élevée' },
        '68': { pop: '764K', conso: '800 MW', sensitivity: 'Élevée' },
        '88': { pop: '367K', conso: '700 MW', sensitivity: 'Très élevée' }
    };
    
    const data = deptData[deptNumber] || {};
    
    // Update the details box
    infoCard.innerHTML = `
        <h3 class="details-title">${deptNumber} - ${deptName}</h3>
        <div style="margin-top: 1.5rem;">
            <div style="display: flex; justify-content: space-between; padding: 0.75rem 0; border-bottom: 1px solid var(--gray-200);">
                <span style="color: var(--gray-600);">Population :</span>
                <strong style="color: var(--gray-900);">${data.pop || 'N/A'}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; padding: 0.75rem 0; border-bottom: 1px solid var(--gray-200);">
                <span style="color: var(--gray-600);">Consommation moyenne :</span>
                <strong style="color: var(--gray-900);">${data.conso || 'N/A'}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; padding: 0.75rem 0;">
                <span style="color: var(--gray-600);">Sensibilité météo :</span>
                <strong style="color: var(--gray-900);">${data.sensitivity || 'N/A'}</strong>
            </div>
        </div>
        <a href="explorer.html?dept=${deptNumber}" class="btn btn-primary" style="margin-top: 1.5rem; width: 100%; text-align: center; display: block;">
            Analyser ce département
        </a>
    `;
    
    // Highlight the selected department
    document.querySelectorAll('.department-area').forEach(dept => {
        dept.style.opacity = '0.7';
        dept.style.filter = 'brightness(1)';
    });
    
    const selectedDept = document.getElementById(`dept-${deptNumber}`);
    if (selectedDept) {
        selectedDept.style.opacity = '1';
        selectedDept.style.filter = 'brightness(1.2)';
        selectedDept.style.strokeWidth = '3';
    }
    
    showNotification(`Département ${deptNumber} - ${deptName} sélectionné`, 'info');
}

// === Filter Functions ===
function selectAll() {
    const checkboxes = document.querySelectorAll('.dept-checkbox input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = true;
    });
    showNotification('Tous les départements sélectionnés', 'success');
}

function selectNone() {
    const checkboxes = document.querySelectorAll('.dept-checkbox input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = false;
    });
    showNotification('Aucun département sélectionné', 'info');
}

function selectMountain() {
    const checkboxes = document.querySelectorAll('.dept-checkbox input[type="checkbox"]');
    const mountainDepts = ['88', '68', '52', '54'];
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = mountainDepts.includes(checkbox.value);
    });
    showNotification('Départements montagneux sélectionnés', 'success');
}

function applyFilters() {
    // Get selected departments
    const checkboxes = document.querySelectorAll('.dept-checkbox input[type="checkbox"]:checked');
    const selectedDepts = Array.from(checkboxes).map(checkbox => checkbox.value);
    
    // Get date range
    const dateStart = document.getElementById('date-start')?.value;
    const dateEnd = document.getElementById('date-end')?.value;
    
    // Get season
    const activeSeasonBtn = document.querySelector('.season-option-btn.active-season, .season-btn.active');
    const activeSeason = activeSeasonBtn?.getAttribute('data-season') || 'all';
    
    // Get day type
    const activeDayTypeBtn = document.querySelector('.daytype-option-btn.active-daytype, .day-type-btn.active');
    const activeDayType = activeDayTypeBtn?.getAttribute('data-type') || 'all';
    
    // Update global state
    currentFilters = {
        departments: selectedDepts,
        dateStart: dateStart || currentFilters.dateStart,
        dateEnd: dateEnd || currentFilters.dateEnd,
        season: activeSeason,
        dayType: activeDayType
    };
    
    // Update summary
    updateFilterSummary();
    
    // In real app, this would fetch data from API
    console.log('Filters applied:', currentFilters);
    
    // Show loading animation
    showNotification('Filtres appliqués ! Actualisation des données...', 'success');
    
    // Simulate data update
    setTimeout(() => {
        animateChartUpdate();
    }, 500);
}

function resetFilters() {
    // Reset to default values
    currentFilters = {
        departments: ['08', '51', '54', '57'],
        dateStart: '2014-01-01',
        dateEnd: '2014-12-31',
        season: 'all',
        dayType: 'all'
    };
    
    // Reset form elements
    const dateStart = document.getElementById('date-start');
    const dateEnd = document.getElementById('date-end');
    
    if (dateStart) dateStart.value = '2014-01-01';
    if (dateEnd) dateEnd.value = '2014-12-31';
    
    // Reset checkboxes
    const checkboxes = document.querySelectorAll('.dept-checkbox input[type="checkbox"]');
    const defaultDepts = ['08', '51', '54', '57'];
    checkboxes.forEach(checkbox => {
        checkbox.checked = defaultDepts.includes(checkbox.value);
    });
    
    // Reset season buttons
    document.querySelectorAll('.season-option-btn, .season-btn').forEach(btn => {
        btn.classList.remove('active-season', 'active');
        if (btn.getAttribute('data-season') === 'all') {
            btn.classList.add('active-season', 'active');
        }
    });
    
    // Reset day type buttons
    document.querySelectorAll('.daytype-option-btn, .day-type-btn').forEach(btn => {
        btn.classList.remove('active-daytype', 'active');
        if (btn.getAttribute('data-type') === 'all') {
            btn.classList.add('active-daytype', 'active');
        }
    });
    
    updateFilterSummary();
    showNotification('Filtres réinitialisés', 'info');
}

function updateFilterSummary() {
    const selectedDeptsEl = document.getElementById('selected-depts');
    const selectedPeriodEl = document.getElementById('selected-period');
    const dataPointsEl = document.getElementById('data-points');
    
    if (selectedDeptsEl) {
        const deptNames = {
            '08': 'Ardennes', '10': 'Aube', '51': 'Marne', '52': 'Haute-Marne',
            '54': 'Meurthe-et-Moselle', '55': 'Meuse', '57': 'Moselle',
            '67': 'Bas-Rhin', '68': 'Haut-Rhin', '88': 'Vosges'
        };
        
        const deptList = currentFilters.departments
            .map(dept => `${dept} - ${deptNames[dept] || dept}`)
            .join(', ');
        
        selectedDeptsEl.textContent = deptList || 'Aucun département sélectionné';
    }
    
    if (selectedPeriodEl) {
        const start = currentFilters.dateStart ? new Date(currentFilters.dateStart).toLocaleDateString('fr-FR') : 'N/A';
        const end = currentFilters.dateEnd ? new Date(currentFilters.dateEnd).toLocaleDateString('fr-FR') : 'N/A';
        selectedPeriodEl.textContent = `${start} - ${end}`;
    }
    
    if (dataPointsEl) {
        // Calculate data points (simplified)
        const startDate = new Date(currentFilters.dateStart);
        const endDate = new Date(currentFilters.dateEnd);
        const days = Math.max(1, Math.floor((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1);
        const points = days * currentFilters.departments.length;
        dataPointsEl.textContent = `${points.toLocaleString('fr-FR')} points`;
    }
}

function animateChartUpdate() {
    // Animate the bars in statistics cards
    document.querySelectorAll('.stat-sparkline').forEach(sparkline => {
        sparkline.style.animation = 'none';
        setTimeout(() => {
            sparkline.style.animation = 'shimmer 2s infinite';
        }, 10);
    });
    
    // Animate chart lines
    const chartLines = document.querySelectorAll('.chart-line');
    chartLines.forEach((line, index) => {
        line.style.strokeDasharray = '2000';
        line.style.strokeDashoffset = '2000';
        line.style.animation = `drawCurve 2.5s ease ${index * 0.3}s forwards`;
    });
}

// === Scenario Calculator ===
function calculateScenario() {
    const temp = parseFloat(document.getElementById('temp-input')?.value || 15);
    const wind = parseFloat(document.getElementById('wind-input')?.value || 15);
    const humidity = parseFloat(document.getElementById('humidity-input')?.value || 70);
    const precipitation = parseFloat(document.getElementById('precipitation-input')?.value || 2);
    const dept = document.getElementById('dept-scenario')?.value || '57';
    const season = document.getElementById('season-scenario')?.value || 'autumn';
    const dayType = document.querySelector('input[name="day-type"]:checked')?.value || 'weekday';
    
    // Simple estimation formula (replace with real ML model)
    let baseConso = 1000; // MW
    
    // Temperature influence (most important)
    const tempDiff = 15 - temp; // Difference from comfort temperature
    baseConso += tempDiff * 25; // 25 MW per degree
    
    // Season adjustment
    const seasonFactors = {
        'winter': 1.2,
        'spring': 0.95,
        'summer': 0.82,
        'autumn': 1.05
    };
    baseConso *= seasonFactors[season] || 1;
    
    // Day type adjustment
    if (dayType === 'weekend') {
        baseConso *= 0.9; // 10% less on weekends
    }
    
    // Department adjustment
    const deptFactors = {
        '08': 0.85, '10': 0.75, '51': 0.9, '52': 0.65,
        '54': 0.95, '55': 0.6, '57': 1.0, '67': 0.92,
        '68': 0.8, '88': 0.7
    };
    baseConso *= deptFactors[dept] || 1;
    
    // Round to nearest 10
    const estimation = Math.round(baseConso / 10) * 10;
    
    // Update results display
    updateEstimationDisplay(estimation, dept, season, dayType, temp);
    
    // Animate the results
    animateNumber('estimation-value', estimation);
}

function updateEstimationDisplay(estimation, dept, season, dayType, temp) {
    const estimationValue = document.querySelector('.value-number');
    const resultDept = document.getElementById('result-dept');
    const resultSeason = document.getElementById('result-season');
    const resultDaytype = document.getElementById('result-daytype');
    const comparisonFill = document.getElementById('comparison-fill');
    const comparisonText = document.getElementById('comparison-text');
    
    if (estimationValue) {
        estimationValue.textContent = estimation;
    }
    
    const deptNames = {
        '08': '08 - Ardennes', '10': '10 - Aube', '51': '51 - Marne',
        '52': '52 - Haute-Marne', '54': '54 - Meurthe-et-Moselle',
        '55': '55 - Meuse', '57': '57 - Moselle', '67': '67 - Bas-Rhin',
        '68': '68 - Haut-Rhin', '88': '88 - Vosges'
    };
    
    const seasonNames = {
        'winter': 'Hiver', 'spring': 'Printemps',
        'summer': 'Été', 'autumn': 'Automne'
    };
    
    const dayTypeNames = {
        'weekday': 'Semaine', 'weekend': 'Week-end'
    };
    
    if (resultDept) resultDept.textContent = deptNames[dept] || dept;
    if (resultSeason) resultSeason.textContent = seasonNames[season] || season;
    if (resultDaytype) resultDaytype.textContent = dayTypeNames[dayType] || dayType;
    
    // Update comparison bar
    const avgConso = 1000; // Average for the department
    const percentage = (estimation / avgConso) * 100;
    const diff = ((estimation - avgConso) / avgConso * 100).toFixed(0);
    
    if (comparisonFill) {
        comparisonFill.style.width = `${Math.min(percentage, 100)}%`;
    }
    
    if (comparisonText) {
        const icon = diff > 0 ? '↑' : '↓';
        const color = diff > 0 ? '#ef4444' : '#10b981';
        comparisonText.innerHTML = `
            <span class="diff-icon">${icon}</span>
            <span class="diff-value" style="color: ${color}">${Math.abs(diff)}%</span>
            <span class="diff-label">par rapport à la moyenne départementale</span>
        `;
    }
    
    // Animate the bar fill
    if (comparisonFill) {
        comparisonFill.style.animation = 'none';
        setTimeout(() => {
            comparisonFill.style.animation = 'fillBar 1.5s ease forwards';
        }, 10);
    }
}

function resetScenario() {
    // Reset to default values
    const tempInput = document.getElementById('temp-input');
    const windInput = document.getElementById('wind-input');
    const humidityInput = document.getElementById('humidity-input');
    const precipitationInput = document.getElementById('precipitation-input');
    
    if (tempInput) tempInput.value = 15;
    if (windInput) windInput.value = 15;
    if (humidityInput) humidityInput.value = 70;
    if (precipitationInput) precipitationInput.value = 2;
    
    const tempDisplay = document.getElementById('temp-display');
    if (tempDisplay) tempDisplay.textContent = '15';
    
    // Update slider color
    if (tempInput) updateSliderColor(tempInput);
    
    showNotification('Scénario réinitialisé', 'info');
}

function loadScenario(scenarioType) {
    const scenarios = {
        'cold': { temp: -5, wind: 25, humidity: 80, precipitation: 0, season: 'winter' },
        'mild': { temp: 18, wind: 10, humidity: 65, precipitation: 0, season: 'spring' },
        'hot': { temp: 33, wind: 5, humidity: 40, precipitation: 0, season: 'summer' },
        'storm': { temp: 8, wind: 70, humidity: 95, precipitation: 15, season: 'autumn' }
    };
    
    const scenario = scenarios[scenarioType];
    if (!scenario) return;
    
    // Set form values
    const tempInput = document.getElementById('temp-input');
    const windInput = document.getElementById('wind-input');
    const humidityInput = document.getElementById('humidity-input');
    const precipitationInput = document.getElementById('precipitation-input');
    const seasonSelect = document.getElementById('season-scenario');
    
    if (tempInput) tempInput.value = scenario.temp;
    if (windInput) windInput.value = scenario.wind;
    if (humidityInput) humidityInput.value = scenario.humidity;
    if (precipitationInput) precipitationInput.value = scenario.precipitation;
    if (seasonSelect) seasonSelect.value = scenario.season;
    
    const tempDisplay = document.getElementById('temp-display');
    if (tempDisplay) tempDisplay.textContent = scenario.temp;
    
    // Update slider color
    if (tempInput) updateSliderColor(tempInput);
    
    // Calculate immediately
    calculateScenario();
    
    showNotification(`Scénario "${scenarioType}" chargé`, 'success');
}

function compareDeparts() {
    const deptA = document.getElementById('dept-a')?.value;
    const deptB = document.getElementById('dept-b')?.value;
    
    if (!deptA || !deptB) {
        showNotification('Veuillez sélectionner deux départements', 'warning');
        return;
    }
    
    if (deptA === deptB) {
        showNotification('Veuillez sélectionner deux départements différents', 'warning');
        return;
    }
    
    showNotification('Comparaison en cours...', 'info');
    
    // In real app, this would fetch data from API
    console.log(`Comparing ${deptA} vs ${deptB}`);
    
    // Simulate comparison results
    setTimeout(() => {
        showNotification('Comparaison terminée', 'success');
    }, 1000);
}

// === Download Functions ===
function downloadData(type) {
    const fileTypes = {
        'aggregated': { name: 'donnees_aggregees_2014.csv', type: 'CSV' },
        'analysis': { name: 'resultats_analyse.json', type: 'JSON' },
        'docs': { name: 'documentation_complete.pdf', type: 'PDF' }
    };
    
    const fileInfo = fileTypes[type] || { name: 'download.txt', type: 'TXT' };
    
    showNotification(`Téléchargement de ${fileInfo.name}...`, 'info');
    
    // In real app, this would trigger actual download
    console.log(`Download ${fileInfo.name}`);
    
    // Simulate download completion
    setTimeout(() => {
        showNotification(`${fileInfo.type} téléchargé avec succès`, 'success');
    }, 1500);
}

// === Helper Functions ===
function updateSliderColor(slider) {
    if (!slider) return;
    
    const value = parseFloat(slider.value);
    const min = parseFloat(slider.min);
    const max = parseFloat(slider.max);
    const percentage = ((value - min) / (max - min)) * 100;
    
    // Create gradient based on temperature value
    let color;
    if (value < 0) color = '#3b82f6'; // Blue for cold
    else if (value < 10) color = '#06b6d4'; // Cyan for cool
    else if (value < 20) color = '#10b981'; // Green for mild
    else if (value < 30) color = '#fbbf24'; // Yellow for warm
    else color = '#ef4444'; // Red for hot
    
    // Update the slider track color (modern browsers)
    slider.style.setProperty('--track-color', color);
    
    // For browsers that support accent-color
    slider.style.accentColor = color;
}

function animateNumber(elementId, targetNumber) {
    const element = document.querySelector(`#${elementId} .value-number`);
    if (!element) return;
    
    const duration = 1000; // 1 second
    const steps = 60;
    const stepDuration = duration / steps;
    const increment = targetNumber / steps;
    
    let current = 0;
    const timer = setInterval(() => {
        current += increment;
        if (current >= targetNumber) {
            current = targetNumber;
            clearInterval(timer);
        }
        element.textContent = Math.round(current);
    }, stepDuration);
}

function showNotification(message, type = 'info') {
    // Remove existing notifications
    document.querySelectorAll('.notification').forEach(n => n.remove());
    
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    
    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        background: white;
        border-radius: 0.5rem;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
        z-index: 9999;
        animation: slideIn 0.3s ease;
        max-width: 400px;
        border-left: 4px solid;
        font-weight: 500;
        font-size: 0.9375rem;
    `;
    
    // Color based on type
    const colors = {
        'success': '#10b981',
        'error': '#ef4444',
        'warning': '#f59e0b',
        'info': '#3b82f6'
    };
    
    notification.style.borderLeftColor = colors[type] || colors.info;
    
    document.body.appendChild(notification);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// === API Simulation (replace with real Spring Boot API calls) ===
async function fetchConsumptionData(filters) {
    // Simulate API call
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                success: true,
                data: [
                    // Sample data structure
                    { date: '2014-01-01', dept: '57', consumption: 1200, temperature: -2 },
                    { date: '2014-01-02', dept: '57', consumption: 1150, temperature: -1 },
                    // ... more data
                ]
            });
        }, 500);
    });
}

async function fetchDepartmentStats(deptCode) {
    // Simulate API call
    return new Promise((resolve) => {
        setTimeout(() => {
            resolve({
                success: true,
                data: {
                    avgConsumption: 950,
                    maxConsumption: 1500,
                    minConsumption: 600,
                    correlation: -0.82
                }
            });
        }, 500);
    });
}

// Add CSS animations if not already present
if (!document.querySelector('#notification-styles')) {
    const style = document.createElement('style');
    style.id = 'notification-styles';
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }
        
        @keyframes drawCurve {
            from {
                stroke-dashoffset: 2000;
            }
            to {
                stroke-dashoffset: 0;
            }
        }
        
        @keyframes fillBar {
            from {
                transform: scaleX(0);
            }
            to {
                transform: scaleX(1);
            }
        }
        
        @keyframes shimmer {
            0% {
                transform: translateX(-100%);
            }
            100% {
                transform: translateX(100%);
            }
        }
    `;
    document.head.appendChild(style);
}

// script.js - Partie pour la page vue-ensemble.html

document.addEventListener('DOMContentLoaded', function() {
    // Données des départements (à remplacer par vos données réelles)
    const deptData = {
        '08': { name: 'Ardennes', conso: '850 MW', pop: '273,000', sensitivity: 'Faible' },
        '10': { name: 'Aube', conso: '750 MW', pop: '310,000', sensitivity: 'Faible' },
        '51': { name: 'Marne', conso: '900 MW', pop: '568,000', sensitivity: 'Faible' },
        '52': { name: 'Haute-Marne', conso: '650 MW', pop: '175,000', sensitivity: 'Moyenne-basse' },
        '54': { name: 'Meurthe-et-Moselle', conso: '950 MW', pop: '733,000', sensitivity: 'Moyenne' },
        '55': { name: 'Meuse', conso: '600 MW', pop: '189,000', sensitivity: 'Moyenne-basse' },
        '57': { name: 'Moselle', conso: '1,000 MW', pop: '1,043,000', sensitivity: 'Moyenne' },
        '67': { name: 'Bas-Rhin', conso: '920 MW', pop: '1,133,000', sensitivity: 'Moyenne-haute' },
        '68': { name: 'Haut-Rhin', conso: '800 MW', pop: '767,000', sensitivity: 'Élevée' },
        '88': { name: 'Vosges', conso: '700 MW', pop: '366,000', sensitivity: 'Élevée' }
    };

    // Gestion des clics sur les départements
    const departments = document.querySelectorAll('.department');
    const deptInfo = document.getElementById('dept-info');
    const deptDetails = document.getElementById('dept-details');
    const selectedDeptName = document.getElementById('selected-dept-name');
    const deptConso = document.getElementById('dept-conso');
    const deptPop = document.getElementById('dept-pop');
    const deptSensitivity = document.getElementById('dept-sensitivity');

    departments.forEach(dept => {
        dept.addEventListener('click', function() {
            const deptCode = this.getAttribute('data-dept');
            const deptName = this.getAttribute('data-name');
            
            // Mettre à jour les informations
            if (deptData[deptCode]) {
                selectedDeptName.textContent = `${deptCode} - ${deptName}`;
                deptConso.textContent = deptData[deptCode].conso;
                deptPop.textContent = deptData[deptCode].pop;
                deptSensitivity.textContent = deptData[deptCode].sensitivity;
                
                // Afficher les détails
                deptDetails.style.display = 'block';
                deptInfo.querySelector('.info-placeholder').style.display = 'none';
            }
            
            // Mettre en surbrillance le département sélectionné
            departments.forEach(d => {
                d.removeAttribute('data-selected');
                d.style.opacity = '0.7';
            });
            
            this.setAttribute('data-selected', 'true');
            this.style.opacity = '1';
            
            // Mettre à jour le lien "Analyser ce département"
            const analyzeLink = deptDetails.querySelector('a');
            analyzeLink.href = `explorer.html?dept=${deptCode}`;
        });
        
        dept.addEventListener('mouseenter', function() {
            if (!this.hasAttribute('data-selected')) {
                this.style.opacity = '1';
            }
        });
        
        dept.addEventListener('mouseleave', function() {
            if (!this.hasAttribute('data-selected')) {
                this.style.opacity = '0.7';
            }
        });
    });

    // Gestion des cases à cocher pour la comparaison
    const checkboxes = document.querySelectorAll('.dept-checkbox-card input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateComparisonChart();
        });
    });

    function updateComparisonChart() {
        // À implémenter : mettre à jour les graphiques en fonction des départements sélectionnés
        console.log('Mise à jour des graphiques de comparaison');
    }

    // Initialiser avec un département par défaut
    if (departments.length > 0) {
        departments[0].click();
    }
});