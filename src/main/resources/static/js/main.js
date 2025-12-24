import * as MapCore from './modules/map-core.js';
import * as Api from './modules/api.js';
import * as Elevation from './modules/elevation.js';
import * as Waypoints from './modules/waypoints.js';
import * as Ui from './modules/ui.js';
import * as Cache from './modules/cache.js';

const state = {
    waypoints: [],
    nextId: 1,
    isElevationOpen: false,
    routeTitle: "",

    currentTrackPoints: null,
    currentClimbs: null,

    isElevationRelevant: false,

    routeDistance: 0,
    totalAscent: null, // <--- ВИПРАВЛЕНО: Було routeAccent (0), стало totalAscent (null)
    routeDuration: 0
};

let fetchTimeout = null;

document.addEventListener('DOMContentLoaded', () => {
    // Карта, обробка ЛКМ - додатиТочку()
    MapCore.initMap('map', (lat, lng) => addWaypoint(lat, lng));

    // Список точок маршруту (Drag&Drop List)
    Waypoints.initSortable((oldIdx, newIdx) => {
        const item = state.waypoints.splice(oldIdx, 1)[0];
        state.waypoints.splice(newIdx, 0, item);

        refreshUi();
        triggerRouteCalculation();
    });

    // Додавання проміжної точки (Drag&Drop Line)
    MapCore.initRouteDragSystem((lat, lng) => {
        insertWaypointAtSmartIndex(lat, lng);
    });

    const qrModal = document.getElementById('qrModal');
    if (qrModal) {
        qrModal.addEventListener('click', closeQrModalHandler);
    }

    Ui.bindUiActions({
        onAddPointMode: () => alert("Натисніть на карту!"),
        onReverse: reverseRoute,
        onExport: exportHandler,
        onShareQr: shareRouteQrHandler,
        onCloseQrModal: closeQrModalHandler,
        onTitleChange: (val) => {
            state.routeTitle = val;
        },

        // Логіка перемикача панелі
        onToggleElevation: () => {
            const panel = document.getElementById('elevation-panel');
            const sidebar = document.querySelector('.sidebar');
            const isExpandedMode = panel.classList.contains('expanded');

            if (isExpandedMode) {
                panel.classList.remove('expanded');
                if (sidebar) sidebar.classList.remove('shrunk');
                const btnIcon = document.getElementById('expand-icon');
                if(btnIcon) btnIcon.className = 'fas fa-expand';
            }

            state.isElevationOpen = !state.isElevationOpen;
            Ui.setElevationPanelState(state.isElevationOpen);

            if (state.isElevationOpen) {
                if (!state.isElevationRelevant) {
                    fetchAndRenderElevation();
                } else {
                    renderElevationUI();
                }
            } else {
                MapCore.clearClimbs();
            }
            setTimeout(() => Elevation.resizeChart(), 310);
        },

        // Логіка розгортання графіку
        onExpandElevationChart: (e) => {
            if (e) e.stopPropagation();

            const isExpanded = Ui.toggleChartExpand();

            const sidebar = document.querySelector('.sidebar');
            if (sidebar) {
                if (isExpanded) sidebar.classList.add('shrunk');
                else sidebar.classList.remove('shrunk');
            }

            if (isExpanded && !state.isElevationOpen) {
                state.isElevationOpen = true;
                Ui.setElevationPanelState(true);
                if (!state.isElevationRelevant) fetchAndRenderElevation();
                else renderElevationUI();
            }
            setTimeout(() => Elevation.resizeChart(), 310);
        }
    });
});

// === ЛОГІКА ТОЧОК ===
function addWaypoint(lat, lng, insertIndex = null, isSilent = false) {
    const id = state.nextId++;
    const wpObj = Waypoints.createWaypoint(id, lat, lng, state.waypoints.length + 1, {
        onDrag: (id, newLat, newLng) => {
            const wp = state.waypoints.find(p => p.id === id);
            if (wp) {
                wp.lat = newLat;
                wp.lng = newLng;
            }
            triggerRouteCalculation();
        }, onRemove: (id) => removeWaypoint(id)
    });

    if (insertIndex !== null) state.waypoints.splice(insertIndex, 0, wpObj); else state.waypoints.push(wpObj);

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
    Waypoints.renderSidebar(state.waypoints, (id) => removeWaypoint(id), (id, val) => {
        const wp = state.waypoints.find(p => p.id === id);
        if (wp) wp.name = val;
    });
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
        Ui.updateRouteInfo(0, 0, null); // Передаємо null, щоб прибрати іконку гори
        Ui.setPanelVisibility(false);
        MapCore.drawPolyline([], null);
        state.currentTrackPoints = [];
        state.currentClimbs = null;
        state.totalAscent = null; // Скидаємо набір
        state.isElevationRelevant = true;
        return;
    }

    const cachedEntry = Cache.get(state.waypoints);

    if (cachedEntry) {
        console.log("Знайдено в кеші");
        applyRouteGeometry(cachedEntry);

        if (Cache.hasElevationData(cachedEntry)) {
            console.log("...з висотами");
            state.currentClimbs = cachedEntry.climbs;
            state.isElevationRelevant = true;

            if (state.isElevationOpen) {
                renderElevationUI();
            }
        } else {
            console.log("...тільки геометрія");
            state.currentClimbs = null;
            state.isElevationRelevant = false;

            if (state.isElevationOpen) {
                fetchAndRenderElevation();
            }
        }
        return;
    }

    fetchTimeout = setTimeout(async () => {
        try {
            const data = await Api.calculateRouteApi(state.waypoints);

            Cache.set(state.waypoints, data);

            state.currentTrackPoints = data.trackPoints;
            state.currentClimbs = null;
            state.isElevationRelevant = false;

            applyRouteGeometry(data);

            if (state.isElevationOpen) {
                fetchAndRenderElevation();
            } else {
                MapCore.clearClimbs();
            }

        } catch (e) {
            console.error(e);
            alert("Помилка побудови: " + e.message);
        }
    }, 500);
}

// Застосовує геометрію і показує кнопку панелі
function applyRouteGeometry(data) {
    state.currentTrackPoints = data.trackPoints;
    state.routeDistance = data.distanceMeters;

    // ВИПРАВЛЕНО: Використовуємо правильну змінну totalAscent
    // Якщо data.totalAscent є (з кешу), беремо його, інакше null
    state.totalAscent = (data.totalAscent !== undefined && data.totalAscent !== null) ? data.totalAscent : null;

    state.routeDuration = data.durationSeconds;

    MapCore.drawPolyline(data.trackPoints, (latLng) => {
        addWaypoint(latLng.lat, latLng.lng, state.waypoints.length - 1);
    });

    // ВИПРАВЛЕНО: Передаємо state.totalAscent
    Ui.updateRouteInfo(state.routeDistance, state.routeDuration, state.totalAscent);

    Ui.setPanelVisibility(true);
}

// Дозавантажує висоти і оновлює кеш
async function fetchAndRenderElevation() {
    if (!state.currentTrackPoints || state.currentTrackPoints.length === 0) return;

    try {
        const elevationData = await Api.getElevationApi(state.currentTrackPoints);

        console.log("Elevation Data received:", elevationData);

        // 1. Оновлюємо стейт
        state.currentClimbs = elevationData.climbs;

        // ВИПРАВЛЕНО: Записуємо отриманий набір у стейт!
        state.totalAscent = elevationData.totalAscent;

        if (elevationData.trackPoints) {
            state.currentTrackPoints = elevationData.trackPoints;
        }

        state.isElevationRelevant = true;

        Cache.updateWithElevation(state.waypoints, elevationData);

        // ВИПРАВЛЕНО: Викликаємо оновлення UI з новим значенням totalAscent
        Ui.updateRouteInfo(state.routeDistance, state.routeDuration, state.totalAscent);

        if (state.isElevationOpen) {
            renderElevationUI();
        }

    } catch (e) {
        state.isElevationRelevant = false;
        console.error("Не вдалося завантажити висоти", e);
    }
}

function renderElevationUI() {
    Elevation.updateElevation(
        state.currentTrackPoints,
        state.currentClimbs,
        state.isElevationOpen
    );
    MapCore.drawClimbs(state.currentClimbs, state.currentTrackPoints);
}

// ... далі exportHandler, shareRouteQrHandler, insertWaypointAtSmartIndex ...
// ... у них змін немає, окрім того, що при експорті теж можна використовувати state.totalAscent якщо треба ...

async function exportHandler() {
    if (!state.currentTrackPoints || state.currentTrackPoints.length < 2) {
        alert("Побудуйте маршрут!");
        return;
    }

    if (!state.isElevationRelevant) {
        try {
            console.log("Експорт: Примусове завантаження актуальних даних висот...");
            await fetchAndRenderElevation();

            if (!state.isElevationRelevant) {
                throw new Error("Не вдалося отримати актуальні дані висот.");
            }
        } catch (e) {
            console.error(e);
            alert("Помилка експорту: " + e.message);
            return;
        }
    }

    try {
        const km = Math.round(state.routeDistance / 1000);
        const hours = Math.round(state.routeDuration / 3600);

        const exportRequestDTO = {
            waypoints: state.currentTrackPoints,
            routeName: state.routeTitle,
            totalDistanceKm: km,
            totalTimeHours: hours
        };
        const { blob, filename } = await Api.exportRouteApi(exportRequestDTO);

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;

        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

    } catch (e) {
        console.error(e);
        alert("Помилка експорту: " + (e.message || "Невідома помилка"));
    }
}

async function shareRouteQrHandler() {
    if (!state.currentTrackPoints || state.currentTrackPoints.length < 2) {
        alert("Побудуйте маршрут!");
        return;
    }

    if (!state.isElevationRelevant) {
        try {
            console.log("Експорт: Примусове завантаження актуальних даних висот...");
            await fetchAndRenderElevation();
            if (!state.isElevationRelevant) {
                throw new Error("Не вдалося отримати актуальні дані висот.");
            }
        } catch (e) {
            console.error(e);
            alert("Помилка експорту: " + e.message);
            return;
        }
    }

    try {
        const exportRequestData = {
            waypoints: state.currentTrackPoints,
            routeName: state.routeTitle,
            includeElevation: true
        };

        const routeId = await Api.shareRouteApi(exportRequestData);
        const downloadUrl = `${window.location.origin}/api/route/download/${routeId}`;
        const modal = document.getElementById('qrModal');
        const qrContainer = document.getElementById('qrcode');

        qrContainer.innerHTML = "";
        new QRCode(qrContainer, { text: downloadUrl, width: 200, height: 200 });
        modal.style.display = "flex";
    } catch (e) {
        console.error(e);
        alert("Не вдалося створити QR код.");
    }
}

function closeQrModalHandler(e) {
    if (!e || e.target.id === 'qrModal') {
        document.getElementById('qrModal').style.display = "none";
    }
}

function insertWaypointAtSmartIndex(newLat, newLng) {
    if (state.waypoints.length < 2) {
        addWaypoint(newLat, newLng);
        return;
    }

    let bestIndex = 0;
    let minDetour = Infinity;

    for (let i = 0; i < state.waypoints.length - 1; i++) {
        const wpA = state.waypoints[i];
        const wpB = state.waypoints[i + 1];

        const distA_New = MapCore.getDistance(wpA.lat, wpA.lng, newLat, newLng);
        const distNew_B = MapCore.getDistance(newLat, newLng, wpB.lat, wpB.lng);
        const distA_B   = MapCore.getDistance(wpA.lat, wpA.lng, wpB.lat, wpB.lng);

        const detour = (distA_New + distNew_B) - distA_B;

        if (detour < minDetour) {
            minDetour = detour;
            bestIndex = i;
        }
    }

    addWaypoint(newLat, newLng, bestIndex + 1);
}