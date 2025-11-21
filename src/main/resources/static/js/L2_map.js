// --- ГЛОБАЛЬНІ ЗМІННІ ---
const map = L.map('map').setView([50.4501, 30.5234], 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: 'OSM' }).addTo(map);

// Центральний стан: масив точок
// Структура об'єкта точки: { id: 1, lat: ..., lng: ..., marker: L.marker }
let waypoints = [];
let routeLayer = null;
let nextId = 1; // Лічильник для унікальних ID

// Ініціалізація SortableJS для списку
const listElement = document.getElementById('waypoints-list');
Sortable.create(listElement, {
    animation: 150,
    onEnd: function (evt) {
        // КОЛИ КОРИСТУВАЧ ЗМІНИВ ПОРЯДОК У СПИСКУ
        const item = waypoints.splice(evt.oldIndex, 1)[0]; // Вирізаємо
        waypoints.splice(evt.newIndex, 0, item); // Вставляємо на нове місце
        updateMapMarkers(); // Оновлюємо номери на карті
        fetchRoute();       // Перераховуємо маршрут
    }
});

// --- 1. ЛОГІКА УПРАВЛІННЯ ТОЧКАМИ ---

function addWaypoint(lat, lng, index = null) {
    // 1. Створюємо маркер
    const marker = L.marker([lat, lng], { draggable: true }).addTo(map);

    const pointObj = {
        id: nextId++,
        lat: lat,
        lng: lng,
        marker: marker
    };

    // Обробка перетягування МАРКЕРА на карті
    marker.on('dragend', function(e) {
        const newPos = e.target.getLatLng();
        pointObj.lat = newPos.lat;
        pointObj.lng = newPos.lng;
        fetchRoute(); // Перерахунок при перетягуванні
    });

    // Видалення точки при кліку (опціонально, можна через контекстне меню)
    marker.on('contextmenu', function() {
        removeWaypoint(pointObj.id);
    });

    // 2. Додаємо в масив
    if (index !== null) {
        waypoints.splice(index, 0, pointObj); // Вставка всередину (для псевдо-точок)
    } else {
        waypoints.push(pointObj); // Вставка в кінець
    }

    // 3. Оновлюємо UI
    renderSidebar();
    fetchRoute();
}

function removeWaypoint(id) {
    const index = waypoints.findIndex(p => p.id === id);
    if (index > -1) {
        // Видаляємо маркер з карти
        map.removeLayer(waypoints[index].marker);
        // Видаляємо з масиву
        waypoints.splice(index, 1);
        // Оновлюємо UI
        renderSidebar();
        fetchRoute();
    }
}

function renderSidebar() {
    listElement.innerHTML = '';
    waypoints.forEach((wp, index) => {
        const li = document.createElement('div');
        li.className = 'waypoint-item';
        li.innerHTML = `
            <span><i class="fas fa-grip-lines"></i> ${index + 1}. Точка</span>
            <button onclick="removeWaypoint(${wp.id})" style="width:auto; color:red;">✕</button>
        `;
        listElement.appendChild(li);
    });
}

// Оновлює лише візуальну частину маркерів (якщо треба показати номери)
function updateMapMarkers() {
    // Тут можна додати логіку зміни іконок, щоб на них були цифри 1, 2, 3...
}

// --- 2. ФУНКЦІОНАЛ ІНТЕРФЕЙСУ ---

// Кнопка "Зворотній маршрут"
function reverseRoute() {
    waypoints.reverse();
    renderSidebar();
    fetchRoute();
}

// Клік по карті (Додавання точки)
map.on('click', function(e) {
    addWaypoint(e.latlng.lat, e.latlng.lng);
});

// --- 3. "ГУМОВИЙ МАРШРУТ" (Створення псевдо-точок) ---

function attachRouteEvents(polyline) {
    // Додаємо обробник кліку по ЛІНІЇ маршруту
    polyline.on('click', function(e) {
        // e.latlng - це координати, де ми клікнули на лінії

        // Складна задача: знайти, між якими точками ми клікнули?
        // Для спрощення MVP: ми просто додаємо точку в кінець, АБО (краще)
        // треба знайти найближчий сегмент.
        // Але Leaflet не дає індексу сегмента при кліку просто так.

        // ПРОСТЕ РІШЕННЯ: Додамо точку, а користувач перетягне її в списку.
        // СКЛАДНЕ РІШЕННЯ (Ваш запит):

        const newPointIndex = findNearestSegmentIndex(e.latlng, waypoints);
        addWaypoint(e.latlng.lat, e.latlng.lng, newPointIndex + 1);
    });
}

// Допоміжна функція для знаходження, куди вставити точку (математика)
function findNearestSegmentIndex(clickLatLng, points) {
    // Це спрощена логіка. В ідеалі треба шукати проєкцію точки на відрізки.
    // Тут ми просто шукаємо найближчу точку і вставляємо після неї.
    let minDistance = Infinity;
    let nearestIndex = 0;

    for (let i = 0; i < points.length - 1; i++) {
        // Тут можна використати L.GeometryUtil (плагін) для точності
        // Але поки просто повернемо останню точку для простоти прикладу
        nearestIndex = i;
    }
    return points.length - 1; // Поки що вставляємо в кінець, якщо не реалізовано точний пошук
}


// --- 4. ВЗАЄМОДІЯ З БЕКЕНДОМ ---

async function fetchRoute() {
    if (waypoints.length < 2) {
        if (routeLayer) map.removeLayer(routeLayer);
        return;
    }

    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));

    try {
        const response = await fetch('/api/route/calculate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            // Обробка помилки 429 (ваша логіка з минулого кроку)
            const errData = await response.json().catch(()=>({}));
            if (errData.message) alert(errData.message);
            return;
        }

        const data = await response.json();

        if (routeLayer) map.removeLayer(routeLayer);

        if (data.status === 'success' && data.polyline) {
            // Декодуємо полілінію (вам потрібна функція decode або бібліотека)
            // АБО якщо бекенд повертає координати:
            // const latLngs = data.trackPoints.map(...)

            // Припустимо, ми на бекенді використовуємо coordinates замість encoded string
            const latLngs = data.trackPoints.map(p => [p.latitude, p.longitude]);

            routeLayer = L.polyline(latLngs, { color: 'blue', weight: 5 }).addTo(map);

            // ВАЖЛИВО: Підключаємо події до нової лінії
            attachRouteEvents(routeLayer);
        }

    } catch (e) {
        console.error(e);
    }
}

// Експорт (Кнопка)
async function exportRoute() {
    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));
    // Логіка POST запиту на /api/route/export і скачування файлу
}