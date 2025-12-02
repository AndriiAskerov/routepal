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
    routeDuration: 0
};

let fetchTimeout = null;

document.addEventListener('DOMContentLoaded', () => {
    // Карта, обробка ЛКМ - додатиТочку()
    MapCore.initMap('map', (lat, lng) => addWaypoint(lat, lng));

    // Список точок маршруту (Drag&Drop)
    Waypoints.initSortable((oldIdx, newIdx) => {
        const item = state.waypoints.splice(oldIdx, 1)[0];
        state.waypoints.splice(newIdx, 0, item);

        refreshUi();
        triggerRouteCalculation();
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
                // 1. Прибираємо клас розширення панелі
                panel.classList.remove('expanded');

                // 2. Повертаємо сайдбар на повну висоту
                if (sidebar) sidebar.classList.remove('shrunk');

                // 3. Повертаємо іконку кнопки розширення в початковий стан
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

            // Якщо розгорнули, то примусово відкриваємо панель (якщо була закрита)
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

    // Якщо точок замало - очищаємо все
    if (state.waypoints.length < 2) {
        Ui.updateRouteInfo(0, 0);
        Ui.setPanelVisibility(false); // Ховаємо панель
        MapCore.drawPolyline([], null);
        state.currentTrackPoints = [];
        state.currentClimbs = null;
        state.isElevationRelevant = true;
        return;
    }

    // 1. Перевірка Кешу через новий метод
    const cachedEntry = Cache.get(state.waypoints);

    if (cachedEntry) {
        console.log("Знайдено в кеші");
        applyRouteGeometry(cachedEntry);

        // Перевіряємо, чи є в кеші висоти
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

            // Якщо користувач вже тримає панель відкритою - докачуємо
            if (state.isElevationOpen) {
                fetchAndRenderElevation();
            }
        }
        return;
    }

    // 2. Якщо в кеші пусто - робимо запит
    fetchTimeout = setTimeout(async () => {
        try {
            // Лише геометрія (швидко)
            const data = await Api.calculateRouteApi(state.waypoints);

            // Зберігаємо в кеш
            Cache.set(state.waypoints, data);

            // Оновлюємо стейт
            state.currentTrackPoints = data.trackPoints;
            state.currentClimbs = null;
            state.isElevationRelevant = false;

            applyRouteGeometry(data);

            // Якщо панель відкрита - докачуємо висоти
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
    state.routeDuration = data.durationSeconds;

    MapCore.drawPolyline(data.trackPoints, (latLng) => {
        addWaypoint(latLng.lat, latLng.lng, state.waypoints.length - 1);
    });
    Ui.updateRouteInfo(data.distanceMeters, data.durationSeconds);

    Ui.setPanelVisibility(true);
}

// Дозавантажує висоти і оновлює кеш
async function fetchAndRenderElevation() {
    // Перевірка на наявність точок
    if (!state.currentTrackPoints || state.currentTrackPoints.length === 0) return;

    try {
        const elevationData = await Api.getElevationApi(state.currentTrackPoints);

        // === ВИПРАВЛЕННЯ ===
        // 1. Оновлюємо стейт НЕГАЙНО, бо дані ми отримали успішно
        state.currentClimbs = elevationData.climbs;

        if (elevationData.trackPoints) {
            state.currentTrackPoints = elevationData.trackPoints;
        }

        // Встановлюємо прапорець: дані є і вони свіжі!
        state.isElevationRelevant = true;

        // 2. Оновлюємо кеш "фоново" (якщо вдасться - супер, ні - не страшно для поточної сесії)
        Cache.updateWithElevation(state.waypoints, elevationData);

        // 3. Малюємо дані, але тільки якщо користувач все ще тримає панель відкритою
        if (state.isElevationOpen) {
            renderElevationUI();
        }

    } catch (e) {
        // Якщо помилка - скидаємо прапорець, щоб спробувати наступного разу
        state.isElevationRelevant = false;
        console.error("Не вдалося завантажити висоти", e);
    }
}

// Просто малює висоти (використовуючи дані зі STATE)
function renderElevationUI() {
    // Тепер передаємо ТРИ аргументи: точки, підйоми, стан панелі
    Elevation.updateElevation(
        state.currentTrackPoints,
        state.currentClimbs,
        state.isElevationOpen
    );

    // Малюємо червоні лінії на карті
    MapCore.drawClimbs(state.currentClimbs, state.currentTrackPoints);
}

// === ІНШЕ (ЕКСПОРТ та QR) ===
async function exportHandler() {
    // 1. Перевірка на мінімальну кількість точок
    if (!state.currentTrackPoints || state.currentTrackPoints.length < 2) {
        alert("Побудуйте маршрут!"); // Або "Маршрут ще не готовий"
        return;
    }

    // Якщо дані висот не актуальні (прапорець false)
    if (!state.isElevationRelevant) {
        try {
            // Примусово завантажуємо висоти та чекаємо результат
            console.log("Експорт: Примусове завантаження актуальних даних висот...");
            await fetchAndRenderElevation();

            // Якщо після завантаження дані все ще неактуальні (наприклад, помилка API),
            // то ми не можемо експортувати.
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
        a.download = filename; // <--- Використовуємо ім'я з DTO

        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

    } catch (e) {
        console.error(e);
        alert("Помилка експорту: " + (e.message || "Невідома помилка"));
    }
}

// Обробник натискання "QR Експорт"
async function shareRouteQrHandler() {
    if (!state.currentTrackPoints || state.currentTrackPoints.length < 2) {
        alert("Побудуйте маршрут!"); // Або "Маршрут ще не готовий"
        return;
    }

    // Якщо дані висот не актуальні (прапорець false)
    if (!state.isElevationRelevant) {
        try {
            // Примусово завантажуємо висоти та чекаємо результат
            console.log("Експорт: Примусове завантаження актуальних даних висот...");
            await fetchAndRenderElevation();

            // Якщо після завантаження дані все ще неактуальні (наприклад, помилка API),
            // то ми не можемо експортувати.
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

        // 3. Отримуємо ID від сервера (Api.shareRouteApi має бути оновлено)
        const routeId = await Api.shareRouteApi(exportRequestData);

        // 4. Формуємо URL та відображаємо QR
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

// Обробник закриття модалки
function closeQrModalHandler(e) {
    if (!e || e.target.id === 'qrModal') {
        document.getElementById('qrModal').style.display = "none";
    }
}