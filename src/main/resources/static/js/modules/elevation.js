/**
 * elevation.js
 * Відповідає за візуалізацію панелі висот.
 */
import { getElevationApi } from './api.js';
import * as MapCore from './map-core.js'; // Виклик підсвітки

let chartInstance = null;

/**
 * Оновлює графік та список підйомів на основі отриманих даних.
 * @param {Array} trackPoints - Точки маршруту (вже з висотою Z)
 * @param {Array} climbs - Список підйомів
 * @param {Boolean} isPanelOpen - Чи відкрита панель
 */
export function updateElevation(trackPoints, climbs, isPanelOpen) {
    if (!isPanelOpen || !trackPoints) return;

    // 1. Рендеримо графік
    renderChart(trackPoints);

    // 2. Рендеримо список (передаємо climbs, які отримали з main.js)
    renderClimbsList(climbs, trackPoints);
}

function renderClimbsList(climbs, allPoints) {
    let listContainer = document.querySelector('.climbs-list');

    // Якщо climbs null або undefined, вважаємо що це пустий масив
    const safeClimbs = climbs || [];

    if (safeClimbs.length === 0) {
        if(listContainer) listContainer.innerHTML = '<div style="padding:10px; color:#999; text-align:center; font-size: 0.9em">Рівнина... 😴<br><span style="font-size:0.8em">Або дані ще не завантажились</span></div>';
        return;
    }

    listContainer.innerHTML = '';

    // Заголовок
    const title = document.createElement('div');
    title.className = 'climbs-title';
    title.innerText = `Знайдено підйомів: ${safeClimbs.length}`;
    listContainer.appendChild(title);

    safeClimbs.forEach((climb, index) => {
        const el = document.createElement('div');
        el.className = 'climb-item';

        const distKm = (climb.distanceMeters / 1000).toFixed(1);
        const avg = climb.avgGradient.toFixed(1);
        const gain = Math.round(climb.elevationGain);

        el.innerHTML = `
            <div class="climb-info">
                <div>Підйом #${index + 1}</div>
                <div style="color:#888; font-size:0.8em;">${distKm} км • +${gain} м</div>
            </div>
            <div class="grade-badge">${avg}%</div>
        `;

        el.addEventListener('mouseenter', () => MapCore.highlightSegment(allPoints, climb.startIndex, climb.endIndex));
        el.addEventListener('click', () => MapCore.highlightSegment(allPoints, climb.startIndex, climb.endIndex));

        listContainer.appendChild(el);
    });

    listContainer.addEventListener('mouseleave', () => MapCore.clearHighlight());
}

function renderChart(points) {
    const ctx = document.getElementById('elevationChart').getContext('2d');

    const labels = [];
    const dataVals = points.map(p => p.elevation); // ORS повертає elevation в p.elevation або p[2]

    // Розрахунок дистанції для осі X
    let dist = 0;
    for (let i = 0; i < points.length; i++) {
        if (i > 0) {
            const p1 = L.latLng(points[i-1].latitude, points[i-1].longitude);
            const p2 = L.latLng(points[i].latitude, points[i].longitude);
            dist += p1.distanceTo(p2);
        }
        labels.push((dist / 1000).toFixed(1));
    }

    if (chartInstance) {
        chartInstance.data.labels = labels;
        chartInstance.data.datasets[0].data = dataVals;
        chartInstance.update();
    } else {
        // ... (створення chartInstance без змін) ...
        chartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Висота (м)',
                    data: dataVals,
                    borderColor: '#007bff',
                    backgroundColor: 'rgba(0, 123, 255, 0.2)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 0,
                    pointHoverRadius: 6,
                    maintainAspectRatio: false
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                scales: {
                    x: { ticks: { maxTicksLimit: 10, maxRotation: 0 } }, // maxRotation щоб не крутило текст
                    y: { beginAtZero: false, ticks: { maxTicksLimit: 5 } } // менше тіків по Y для економії місця
                },
                plugins: { legend: { display: false } }
            }
        });
    }
}

export function resizeChart() {
    if (chartInstance) chartInstance.resize();
}