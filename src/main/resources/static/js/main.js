import * as MapCore from './modules/map-core.js';
import * as Api from './modules/api.js';
import * as Elevation from './modules/elevation.js';
import * as Waypoints from './modules/waypoints.js';
import * as Ui from './modules/ui.js';
import * as Cache from './modules/cache.js';

// Стан програми
const state = {
    waypoints: [],
    nextId: 1,
    isElevationOpen: false
};

let fetchTimeout = null;

// === ІНІЦІАЛІЗАЦІЯ ===
document.addEventListener('DOMContentLoaded', () => {
    MapCore.initMap('map', (lat, lng) => addWaypoint(lat, lng));

    // Ініціалізація списку та Drag&Drop
    Waypoints.initSortable((oldIdx, newIdx) => {
        const item = state.waypoints.splice(oldIdx, 1)[0];
        state.waypoints.splice(newIdx, 0, item);

        refreshUi();
        triggerRouteCalculation();
    });

    // Біндинг кнопок (ОНОВЛЕНО)
    Ui.bindButtons({
        onAddPointMode: () => alert("Клікніть по карті!"),
        onReverse: reverseRoute,
        onExport: exportHandler,
        onShareQr: shareRouteQrHandler,    // <--- Додано
        onCloseQrModal: closeQrModalHandler // <--- Додано
    });
});

// === ЛОГІКА ТОЧОК ===
function addWaypoint(lat, lng, insertIndex = null, isSilent = false) {
    const id = state.nextId++;
    const wpObj = Waypoints.createWaypoint(
        id, lat, lng,
        state.waypoints.length + 1,
        {
            onDrag: (id, newLat, newLng) => {
                const wp = state.waypoints.find(p => p.id === id);
                if (wp) { wp.lat = newLat; wp.lng = newLng; }
                triggerRouteCalculation();
            },
            onRemove: (id) => removeWaypoint(id)
        }
    );

    if (insertIndex !== null) state.waypoints.splice(insertIndex, 0, wpObj);
    else state.waypoints.push(wpObj);

    refreshUi();
    if (!isSilent) triggerRouteCalculation();
}

function removeWaypoint(id) {
    const idx = state.waypoints.findIndex(wp => wp.id === id);
    if (idx > -1) {
        MapCore.removeLayer(state.waypoints[idx].marker);
        state.waypoints.splice(idx, 1);
        refreshUi();
        triggerRouteCalculation();
    }
}

function refreshUi() {
    Waypoints.renderSidebar(
        state.waypoints,
        (id) => removeWaypoint(id),
        (id, val) => {
            const wp = state.waypoints.find(p => p.id === id);
            if(wp) wp.name = val;
        }
    );
    Waypoints.refreshMarkers(state.waypoints);
}

// === МАРШРУТ І КЕШУВАННЯ ===
function reverseRoute() {
    if (state.waypoints.length < 2) return;
    state.waypoints.reverse();
    refreshUi();
    triggerRouteCalculation();
}

function triggerRouteCalculation() {
    if (fetchTimeout) clearTimeout(fetchTimeout);

    if (state.waypoints.length < 2) {
        Ui.updateRouteInfo(0, 0);
        MapCore.drawPolyline([], null);
        return;
    }

    const cachedData = Cache.get(state.waypoints);
    if (cachedData) {
        console.log("Відновлено з кешу");
        applyRouteData(cachedData);
        return;
    }

    fetchTimeout = setTimeout(async () => {
        try {
            const data = await Api.calculateRouteApi(state.waypoints);
            Cache.set(state.waypoints, data);
            applyRouteData(data);
        } catch (e) {
            console.error(e);
            alert("Помилка побудови: " + e.message);
        }
    }, 500);
}

function applyRouteData(data) {
    // data тепер виглядає так: { trackPoints: [...], climbs: [...] }

    // 1. Малюємо основний синій маршрут
    MapCore.drawPolyline(data.trackPoints, (latLng) => {
        addWaypoint(latLng.lat, latLng.lng, state.waypoints.length - 1);
    });

    Ui.updateRouteInfo(data.distanceMeters, data.durationSeconds); // Якщо ці поля є у відповіді
    // ПРИМІТКА: Якщо distance/duration раніше приходили окремо, переконайтесь,
    // що ваш новий ElevationResponseDTO містить їх, або беріть їх з іншого місця.
    // Якщо ви змінили API так, що тепер повертається тільки ElevationResponseDTO,
    // то вам треба додати поля distance/duration в цей DTO на стороні Java!
    // АБО: повернути старий DTO, який містить всередині elevationData.

    document.getElementById('elevation-panel').style.display = 'flex';

    // Оновлюємо графік
    Elevation.updateElevation(data.trackPoints, state.isElevationOpen);

    // === НОВЕ: Зберігаємо climbs у глобальний стан або передаємо далі ===
    state.currentClimbs = data.climbs;
    state.currentTrackPoints = data.trackPoints;

    if (state.isElevationOpen) {
        MapCore.drawClimbs(state.currentClimbs, state.currentTrackPoints);
    }
}

// === ІНШЕ (ЕКСПОРТ та QR) ===
async function exportHandler() {
    try {
        const blob = await Api.exportRouteApi(state.waypoints);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'route.gpx';
        a.click();
    } catch (e) { alert("Помилка експорту"); }
}

// Обробник натискання "QR Експорт"
async function shareRouteQrHandler() {
    if (state.waypoints.length < 2) {
        alert("Побудуйте маршрут перед експортом!");
        return;
    }

    try {
        // 1. Отримуємо ID
        const routeId = await Api.shareRouteApi(state.waypoints);

        // 2. Формуємо URL (працює і для localhost, і для ngrok)
        const downloadUrl = `${window.location.origin}/api/route/download/${routeId}`;
        console.log("QR Link:", downloadUrl);

        // 3. Відкриваємо модалку
        const modal = document.getElementById('qrModal');
        const qrContainer = document.getElementById('qrcode');

        qrContainer.innerHTML = ""; // Очистка старого QR
        new QRCode(qrContainer, {
            text: downloadUrl,
            width: 200,
            height: 200
        });

        modal.style.display = "flex"; // Використовуємо flex для центрування
    } catch (e) {
        console.error(e);
        alert("Не вдалося створити QR код.");
    }
}

// Обробник закриття модалки
function closeQrModalHandler(e) {
    // Закриваємо, якщо клікнули по фону або по хрестику
    if (e.target.id === 'qrModal' || e.target.classList.contains('close-btn')) {
        document.getElementById('qrModal').style.display = "none";
    }
}

// === ГЛОБАЛЬНІ ФУНКЦІЇ (Для onclick в HTML) ===
// Залишаємо тут, бо вони прив'язані до логіки Elevation, яка керується станом main.js
window.toggleElevationPanel = () => {
    const panel = document.getElementById('elevation-panel');
    const chevron = document.getElementById('elevation-chevron');

    if (panel.classList.contains('expanded')) {
        // Закриття
        panel.classList.remove('expanded', 'open');
        state.isElevationOpen = false;
        chevron.className = 'fas fa-chevron-up';

        MapCore.clearClimbs(); // <--- ПРИБИРАЄМО ЧЕРВОНЕ ПРИ ЗАКРИТТІ
        return;
    }

    // Відкриття
    state.isElevationOpen = !state.isElevationOpen;
    if (state.isElevationOpen) {
        panel.classList.add('open');
        chevron.className = 'fas fa-chevron-down';

        // <--- МАЛЮЄМО, ЯКЩО Є ДАНІ
        if (state.currentClimbs && state.currentTrackPoints) {
            MapCore.drawClimbs(state.currentClimbs, state.currentTrackPoints);
        } else {
            triggerRouteCalculation(); // Якщо даних нема, завантажуємо
        }

    } else {
        panel.classList.remove('open');
        chevron.className = 'fas fa-chevron-up';
        MapCore.clearClimbs(); // <--- ПРИБИРАЄМО
    }
    setTimeout(() => Elevation.resizeChart(), 310);
};

window.toggleExpandChart = (e) => {
    if (e) e.stopPropagation();
    const panel = document.getElementById('elevation-panel');
    panel.classList.toggle('expanded');

    if (panel.classList.contains('expanded')) {
        state.isElevationOpen = true;
        panel.classList.add('open');
        document.getElementById('elevation-chevron').className = 'fas fa-chevron-down';
        triggerRouteCalculation();
    }
    setTimeout(() => Elevation.resizeChart(), 310);
};