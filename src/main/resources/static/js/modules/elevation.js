/**
 * elevation.js
 * Відповідає за Chart.js та логіку панелі висот.
 */
import { getElevationApi } from './api.js';
import * as MapCore from './map-core.js'; // Виклик підсвітки

let chartInstance = null;
let currentTrack = [];
let currentData = null; // Тут зберігаємо { trackPoints, climbs }
// const cache = new Map();

/**
 * Головна функція оновлення даних.
 * @param {Array} trackPoints - Точки маршруту.
 * @param {Boolean} isPanelOpen - Чи відкрита панель зараз.
 */
export async function updateElevation(trackPoints, isPanelOpen) {
    if (!isPanelOpen || !trackPoints || trackPoints.length === 0) return;

    // ... кешування (оновіть логіку кешу, бо структура змінилась) ...
    // Для спрощення, поки без кешу або припускаємо, що getElevationApi повертає об'єкт
    try {
        const data = await getElevationApi(trackPoints);
        currentData = data;

        // 1. Рендеримо графік (передаємо тільки точки)
        renderChart(data.trackPoints);

        // 2. Рендеримо список підйомів
        renderClimbsList(data.climbs, data.trackPoints);

    } catch (e) { console.error(e); }
}

function renderClimbsList(climbs, allPoints) {
    // Спочатку треба створити контейнер в HTML, якщо його немає
    // Але ми зробимо це динамічно у map.html або тут
    let listContainer = document.querySelector('.climbs-list');

    // Якщо немає підйомів
    if (!climbs || climbs.length === 0) {
        if(listContainer) listContainer.innerHTML = '<div style="padding:10px; color:#999; text-align:center;">Рівнина... 😴</div>';
        return;
    }

    listContainer.innerHTML = '';

    // Заголовок
    const title = document.createElement('div');
    title.className = 'climbs-title';
    title.innerText = `Знайдено підйомів: ${climbs.length}`;
    listContainer.appendChild(title);

    climbs.forEach((climb, index) => {
        const el = document.createElement('div');
        el.className = 'climb-item';

        // Форматуємо дані
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

        // Interaction: Hover
        el.addEventListener('mouseenter', () => {
            MapCore.highlightSegment(allPoints, climb.startIndex, climb.endIndex);
        });

        // Interaction: Click (Зум)
        el.addEventListener('click', () => {
            MapCore.highlightSegment(allPoints, climb.startIndex, climb.endIndex);
            // Тут можна додати зум, якщо треба
        });

        listContainer.appendChild(el);
    });

    // При виході мишкою зі списку - очищаємо
    listContainer.addEventListener('mouseleave', () => {
        MapCore.clearHighlight();
    });
}

function renderChart(points) {
    const ctx = document.getElementById('elevationChart').getContext('2d');

    const labels = [];
    const dataVals = points.map(p => p.elevation);
    let dist = 0;

    for (let i = 0; i < points.length; i++) {
        if (i > 0) {
            const p1 = L.latLng(points[i-1].latitude, points[i-1].longitude);
            const p2 = L.latLng(points[i].latitude, points[i].longitude);
            dist += p1.distanceTo(p2);
        }
        labels.push((dist / 1000).toFixed(1));
        dataVals.push(points[i].elevation);
    }

    if (chartInstance) {
        chartInstance.data.labels = labels;
        chartInstance.data.datasets[0].data = dataVals;
        chartInstance.update();
    } else {
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
                    x: { ticks: { maxTicksLimit: 10 } },
                    y: { beginAtZero: false }
                },
                plugins: { legend: { display: false } }
            }
        });
    }
}

export function resizeChart() {
    if (chartInstance) chartInstance.resize();
}