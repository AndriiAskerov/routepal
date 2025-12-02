/**
 * map-core.js
 * Керує екземпляром Leaflet карти, шарами та маркерами.
 */

let mapInstance = null;
let routeLayer = null;
let highlightLayer = null; // Шар підсвітки
let climbsLayerGroup = null;

/**
 * Ініціалізує карту.
 * @param {String} elementId - ID HTML елемента карти.
 * @param {Function} onClickCallback - Функція, що викликається при кліку по карті (lat, lng).
 */
export function initMap(elementId, onClickCallback) {
    mapInstance = L.map(elementId).setView([50.4501, 30.5234], 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: 'OSM'
    }).addTo(mapInstance);

    mapInstance.on('click', (e) => {
        onClickCallback(e.latlng.lat, e.latlng.lng);
    });

    // Ініціалізуємо групу шарів
    climbsLayerGroup = L.layerGroup().addTo(mapInstance);
}

/**
 * Додає маркер на карту.
 * @param {Number} lat
 * @param {Number} lng
 * @param {Number} index - Номер точки (1, 2...).
 * @param {Function} onDragEnd - Колбек при перетягуванні.
 * @param {Function} onRightClick - Колбек при правому кліку (видалення).
 * @returns {L.Marker} Leaflet маркер.
 */
export function addMarkerToMap(lat, lng, index, onDragEnd, onRightClick) {
    const icon = createNumberedIcon(index);
    const marker = L.marker([lat, lng], { draggable: true, icon: icon }).addTo(mapInstance);

    marker.on('dragend', (e) => {
        const pos = e.target.getLatLng();
        onDragEnd(pos.lat, pos.lng);
    });

    marker.on('contextmenu', () => {
        onRightClick();
    });

    return marker;
}

export function removeLayer(layer) {
    if (layer && mapInstance) mapInstance.removeLayer(layer);
}

export function updateMarkerIcon(marker, index) {
    marker.setIcon(createNumberedIcon(index));
    marker.setZIndexOffset(100 + index);
}

/**
 * Малює лінію маршруту.
 * @param {Array} points - Масив точок {latitude, longitude}.
 * @param {Function} onRouteClick - Колбек для додавання точки на лінію.
 */
export function drawPolyline(points, onRouteClick) {
    // 1. Спочатку видаляємо стару лінію (треба завжди)
    if (routeLayer) {
        mapInstance.removeLayer(routeLayer);
        routeLayer = null;
    }

    // 2. Якщо точок немає, або масив порожній — виходимо.
    if (!points || points.length === 0) return;

    const latLngs = points.map(p => [p.latitude, p.longitude]);
    routeLayer = L.polyline(latLngs, { color: 'blue', weight: 5, opacity: 0.7 }).addTo(mapInstance);

    mapInstance.fitBounds(routeLayer.getBounds(), { padding: [50, 50] });

    // Події лінії
    routeLayer.on('mouseover', function() { this.setStyle({ weight: 8 }); });
    routeLayer.on('mouseout', function() { this.setStyle({ weight: 5 }); });
    routeLayer.on('click', (e) => {
        L.DomEvent.stopPropagation(e);
        if (onRouteClick) onRouteClick(e.latlng);
    });
}

// Приватний хелпер
function createNumberedIcon(number) {
    return L.divIcon({
        className: 'custom-marker-icon',
        html: `<div class="marker-circle">${number}</div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
    });
}

/**
 * Підсвічує сегмент маршруту.
 * @param {Array} fullTrackPoints - Повний масив точок.
 * @param {Number} startIndex
 * @param {Number} endIndex
 */
export function highlightSegment(fullTrackPoints, startIndex, endIndex) {
    // 1. Прибираємо стару підсвітку
    if (highlightLayer) {
        mapInstance.removeLayer(highlightLayer);
        highlightLayer = null;
    }

    if (!fullTrackPoints || startIndex < 0) return;

    // 2. Вирізаємо потрібний шматок
    // slice не включає endIndex, тому +1
    const segment = fullTrackPoints.slice(startIndex, endIndex + 1);
    const latLngs = segment.map(p => [p.latitude, p.longitude]);

    // 3. Малюємо товсту червону лінію поверх основної
    highlightLayer = L.polyline(latLngs, {
        color: '#dc3545', // Червоний
        weight: 8,        // Товстіша за маршрут
        opacity: 0.9,
        lineCap: 'round'
    }).addTo(mapInstance);

    // 4. Трохи наближаємо до сегменту (опціонально, можна закоментувати)
    // mapInstance.fitBounds(highlightLayer.getBounds(), { maxZoom: 14, padding: [20, 20] });
}

export function clearHighlight() {
    if (highlightLayer) {
        mapInstance.removeLayer(highlightLayer);
        highlightLayer = null;
    }
}

/**
 * Малює червоні ділянки підйомів та підписи %
 * @param {Array} climbs - масив об'єктів ClimbDTO
 * @param {Array} allPoints - масив усіх точок маршруту {lat, lng}
 */
export function drawClimbs(climbs, allPoints) {
    if (!climbsLayerGroup) return;
    climbsLayerGroup.clearLayers(); // Очищаємо старі підйоми

    if (!climbs || climbs.length === 0) return;

    climbs.forEach(climb => {
        // 1. Витягуємо об'єкти точок для цього шматка
        const segmentPoints = allPoints.slice(climb.startIndex, climb.endIndex + 1);

        if (segmentPoints.length < 2) return;

        // ВАЖЛИВО: Перетворюємо об'єкти {latitude, longitude} у масиви [lat, lng] для Leaflet
        const segmentLatLngs = segmentPoints.map(p => [p.latitude, p.longitude]);

        // 2. Малюємо червону лінію поверх маршруту
        L.polyline(segmentLatLngs, { // Передаємо виправлений масив
            color: '#d90429',
            weight: 6,
            opacity: 0.8
        }).addTo(climbsLayerGroup);

        // 3. Додаємо підпис з % посередині ділянки
        const middleIndex = Math.floor(segmentLatLngs.length / 2);
        const centerPoint = segmentLatLngs[middleIndex]; // Тепер це масив [lat, lng], Leaflet це зрозуміє

        L.tooltip({
            permanent: true,
            direction: 'center',
            className: 'climb-label',
            offset: [0, 0]
        })
            .setContent(`${climb.avgGradient.toFixed(1)}%`)
            .setLatLng(centerPoint)
            .addTo(climbsLayerGroup);
    });
}

// Функція для приховання підйомів (якщо закрили графік)
export function clearClimbs() {
    if (climbsLayerGroup) climbsLayerGroup.clearLayers();
}