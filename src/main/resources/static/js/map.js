// ==========================================
// 1. GLOBALS & CONFIGURATION
// ==========================================
const map = L.map('map').setView([50.4501, 30.5234], 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: 'OSM' }).addTo(map);

const routeCache = new Map();
let waypoints = [];
let routeLayer = null;
let nextId = 1;
let fetchTimeout = null;

// ==========================================
// 2. INITIALIZATION (SortableJS & Events)
// ==========================================
const listElement = document.getElementById('waypoints-list');

Sortable.create(listElement, {
    animation: 150,
    handle: '.handle',

    // --- НОВІ НАЛАШТУВАННЯ ---
    forceFallback: true,        // Вимикаємо нативний Drag&Drop (прибирає системну "примару")
    fallbackClass: 'sortable-drag-clone', // Клас для елемента під курсором
    ghostClass: 'sortable-placeholder',   // Клас для місця вставки (пунктир)
    fallbackOnBody: true,       // Щоб елемент не обрізався межами списку

    onEnd: function (evt) {
        if (evt.oldIndex === evt.newIndex) return;

        const item = waypoints.splice(evt.oldIndex, 1)[0];
        waypoints.splice(evt.newIndex, 0, item);

        refreshUI();
        fetchRoute();
    }
});

// Клік по карті додає точку
map.on('click', (e) => addWaypoint(e.latlng.lat, e.latlng.lng));

// ==========================================
// 3. CORE STATE MANAGEMENT (Add, Remove, Update)
// ==========================================

function addWaypoint(lat, lng, index = null, isSilent = false) {
    const marker = createMarker(lat, lng);

    const pointObj = {
        id: nextId++,
        lat: lat,
        lng: lng,
        marker: marker,
        name: "",
        isCustom: false
    };

    // Вставка в масив (в кінець або за індексом)
    if (index !== null) {
        waypoints.splice(index, 0, pointObj);
    } else {
        waypoints.push(pointObj);
    }

    // Налаштування подій маркера
    setupMarkerEvents(marker, pointObj);

    refreshUI();

    if (!isSilent) {
        fetchRoute();
    }
}

function removeWaypoint(id) {
    const index = waypoints.findIndex(p => p.id === id);
    if (index > -1) {
        map.removeLayer(waypoints[index].marker);
        waypoints.splice(index, 1);

        refreshUI();
        fetchRoute();
    }
}

function reverseRoute() {
    waypoints.reverse();
    refreshUI();
    fetchRoute();
}

function updatePointName(id, newName) {
    const point = waypoints.find(p => p.id === id);
    if (point) {
        point.name = newName;
        point.isCustom = true;
    }
}

// ==========================================
// 4. UI RENDERING & VISUALS
// ==========================================

// Оновлює і список, і маркери на карті
function refreshUI() {
    updateMapMarkers();
    renderSidebar();
}

function renderSidebar() {
    listElement.innerHTML = '';
    const template = document.getElementById('waypoint-template');

    waypoints.forEach((wp, index) => {
        // 1. Клонуємо вміст шаблону
        const clone = template.content.cloneNode(true);

        // 2. Знаходимо елементи всередині клону
        const numberSpan = clone.querySelector('.waypoint-number');
        const nameInput = clone.querySelector('.waypoint-name-input');
        const deleteBtn = clone.querySelector('.btn-delete');

        // 3. Заповнюємо даними
        numberSpan.textContent = `${index + 1}.`;

        // Логіка визначення назви
        let displayName = wp.name;
        if (!wp.isCustom) {
            if (index === 0) displayName = "Старт";
            else if (index === waypoints.length - 1 && waypoints.length > 1) displayName = "Фініш";
            else displayName = `Точка`;
        }
        nameInput.value = displayName;

        // 4. Додаємо обробники подій (Event Listeners)
        nameInput.addEventListener('change', (e) => {
            updatePointName(wp.id, e.target.value);
        });

        nameInput.addEventListener('focus', (e) => {
            e.target.select();
        });

        deleteBtn.addEventListener('click', () => {
            removeWaypoint(wp.id);
        });

        // 5. Додаємо готовий елемент у список
        listElement.appendChild(clone);
    });
}

function updateMapMarkers() {
    waypoints.forEach((wp, index) => {
        const newIcon = createNumberedIcon(index + 1);
        wp.marker.setIcon(newIcon);
        wp.marker.setZIndexOffset(100 + index);
    });
}

function createMarker(lat, lng) {
    return L.marker([lat, lng], { draggable: true }).addTo(map);
}

function setupMarkerEvents(marker, pointObj) {
    // Drag & Drop маркера
    marker.on('dragend', function(e) {
        const newPos = e.target.getLatLng();
        pointObj.lat = newPos.lat;
        pointObj.lng = newPos.lng;
        fetchRoute();
    });

    // Context Menu (ПКМ)
    marker.on('contextmenu', function() {
        removeWaypoint(pointObj.id);
    });
}

// ==========================================
// 5. API INTERACTION (Fetch, Debounce, Cache)
// ==========================================

async function fetchRoute() {
    // Debounce: скидаємо попередній таймер
    if (fetchTimeout) clearTimeout(fetchTimeout);

    if (waypoints.length < 2) {
        clearRouteLayer();
        document.getElementById('route-info').innerHTML = '';
        return;
    }

    const cacheKey = generateCacheKey(waypoints);

    // Перевірка кешу (миттєве малювання)
    if (routeCache.has(cacheKey)) {
        console.log("Маршрут з кешу");
        drawRoute(routeCache.get(cacheKey));
        return;
    }

    // Відкладений запит
    fetchTimeout = setTimeout(async () => {
        const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));

        try {
            const response = await fetch('/api/route/calculate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const errData = await response.json().catch(()=>({}));
                if (errData.message) alert(errData.message);
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                routeCache.set(cacheKey, data);
                drawRoute(data);
            }
        } catch (e) {
            console.error("API Error:", e);
        }
    }, 500); // 500ms затримка
}

async function exportRoute() {
    if (waypoints.length < 2) {
        alert("Для експорту потрібно побудувати маршрут (мінімум 2 точки).");
        return;
    }

    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));

    try {
        const response = await fetch('/api/route/export', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => null);
            if (errData && errData.message) alert(errData.message);
            else alert(`Помилка експорту: ${response.status}`);
            return;
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;

        a.download = makeFileName();
        document.body.appendChild(a);
        a.click();

        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);

    } catch (e) {
        console.error("Export failed:", e);
        alert("Не вдалося експортувати маршрут.");
    }
}

function makeFileName() {
    let finalFileName;
    const titleInput = document.getElementById('route-title-input');

    // 1. Спочатку перевіряємо введення користувача
    if (titleInput && titleInput.value.trim() !== "") {
        let customName = titleInput.value.trim();
        // "Санітизація" (заміна заборонених символів на підкреслення)
        finalFileName = customName.replace(/[\\/:*?"<>|]/g, '_');
    } else {
        // 2. Генеруємо дефолтну назву ТІЛЬКИ якщо поле порожнє
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        finalFileName = `map_${year}.${month}.${day}`;
    }

    // 3. Гарантуємо розширення .gpx
    if (!finalFileName.toLowerCase().endsWith('.gpx')) {
        finalFileName += '.gpx';
    }
    return finalFileName;
}

// ==========================================
// 6. MAP VISUALIZATION & INTERACTION
// ==========================================

function drawRoute(data) {
    clearRouteLayer();

    if (data.trackPoints && data.trackPoints.length > 0) {
        const latLngs = data.trackPoints.map(p => [p.latitude, p.longitude]);

        routeLayer = L.polyline(latLngs, { color: 'blue', weight: 5, opacity: 0.7 }).addTo(map);
        map.fitBounds(routeLayer.getBounds(), { padding: [50, 50] });

        updateRouteInfo(data.distanceMeters, data.durationSeconds);
        attachRouteEvents(routeLayer);
    }
}

function clearRouteLayer() {
    if (routeLayer) map.removeLayer(routeLayer);
}

function updateRouteInfo(distanceMeters, durationSeconds) {
    const distanceKm = (distanceMeters / 1000).toFixed(2);

    // --- ЛОГІКА ФОРМАТУВАННЯ ЧАСУ ---
    const totalMinutes = Math.round(durationSeconds / 60);
    let durationText;

    if (totalMinutes >= 60) {
        const hours = Math.floor(totalMinutes / 60);
        const minutes = totalMinutes % 60;

        if (minutes === 0) {
            durationText = `${hours} год.`;
        } else {
            durationText = `${hours} год. ${minutes} хв.`;
        }
    } else {
        durationText = `${totalMinutes} хв.`;
    }
    // --------------------------------

    document.getElementById('route-info').innerHTML = `
        <div style="margin-top: 10px; padding: 10px; background: #e9ecef; border-radius: 5px;">
            <strong>Відстань:</strong> ${distanceKm} км<br>
            <strong>Час:</strong> ${durationText}
        </div>
    `;
}

function attachRouteEvents(polyline) {
    polyline.on('mouseover', function() { this.setStyle({ weight: 8, cursor: 'pointer' }); });
    polyline.on('mouseout', function() { this.setStyle({ weight: 5 }); });

    // Клік по лінії створює нову точку (Silent Mode)
    polyline.on('click', function(e) {
        L.DomEvent.stopPropagation(e);
        const insertIndex = findBestInsertIndex(e.latlng);
        addWaypoint(e.latlng.lat, e.latlng.lng, insertIndex, true); // true = silent
    });
}

function findBestInsertIndex(clickLatLng) {
    if (waypoints.length < 2) return waypoints.length;
    let minDistance = Infinity;
    let bestIndex = 1;

    for (let i = 0; i < waypoints.length - 1; i++) {
        const p1 = L.latLng(waypoints[i].lat, waypoints[i].lng);
        const p2 = L.latLng(waypoints[i+1].lat, waypoints[i+1].lng);

        const distToP1 = clickLatLng.distanceTo(p1);
        const distToP2 = clickLatLng.distanceTo(p2);
        const segmentLength = p1.distanceTo(p2);

        const detour = (distToP1 + distToP2) - segmentLength;

        if (detour < minDistance) {
            minDistance = detour;
            bestIndex = i + 1;
        }
    }
    return bestIndex;
}

// ==========================================
// 7. UTILITIES & HELPERS
// ==========================================

function createNumberedIcon(number) {
    return L.divIcon({
        className: 'custom-marker-icon',
        html: `<div class="marker-circle">${number}</div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
    });
}

function generateCacheKey(points) {
    return points.map(p => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`).join('|');
}

// ==========================================
// 8. QR CODE & SHARING
// ==========================================

async function shareRouteQr() {
    if (waypoints.length < 2) {
        alert("Побудуйте маршрут перед експортом (мінімум 2 точки).");
        return;
    }

    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));

    try {
        // 1. Відправляємо точки на сервер, щоб отримати ID
        const response = await fetch('/api/route/share', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            alert("Помилка створення посилання для шарингу");
            return;
        }

        const data = await response.json(); // Очікуємо { "id": "uuid..." }
        const routeId = data.id;

        // 2. Формуємо повне посилання для скачування
        // window.location.origin автоматично підставить localhost або ngrok адресу
        const downloadUrl = `${window.location.origin}/api/route/download/${routeId}`;

        console.log("Download URL:", downloadUrl);

        // 3. Показуємо модальне вікно та генеруємо QR
        showQrModal(downloadUrl);

    } catch (e) {
        console.error("Share failed:", e);
        alert("Не вдалося створити QR код.");
    }
}

function showQrModal(url) {
    const modal = document.getElementById('qrModal');
    const qrContainer = document.getElementById('qrcode');

    // Очищаємо старий код
    qrContainer.innerHTML = "";

    // Генеруємо новий
    new QRCode(qrContainer, {
        text: url,
        width: 200,
        height: 200
    });

    // Показуємо вікно (змінюємо display з none на block, але у нас flex у CSS)
    modal.style.display = "block";
}

function closeQrModal() {
    document.getElementById('qrModal').style.display = "none";
}

// Закриття при кліку поза вікном
window.onclick = function(event) {
    const modal = document.getElementById('qrModal');
    if (event.target === modal) {
        modal.style.display = "none";
    }
}