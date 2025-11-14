// --- 5. Ініціалізація Карти ---

// Встановлюємо початковий вид на Київ
const map = L.map('map').setView([50.4501, 30.5234], 12);

// Додаємо "плитки" (tiles) карти - це те, що ви бачите.
// Ми використовуємо OpenStreetMap (OSM) - той самий, що й ORS.
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

// --- 6. Логіка Побудови Маршруту ---

// Масив для зберігання точок, які вибрав користувач
let waypoints = [];
// Змінна для відображення лінії маршруту
let routeLayer = null;

// Обробник кліку на карті
map.on('click', function (e) {
    const newPoint = e.latlng;

    // Додаємо маркер на карту
    L.marker([newPoint.lat, newPoint.lng]).addTo(map);

    // Додаємо координати у наш масив у потрібному форматі
    waypoints.push({
        latitude: newPoint.lat,
        longitude: newPoint.lng
    });

    // Якщо у нас є 2 або більше точок, ми можемо будувати маршрут
    if (waypoints.length >= 2) {
        // Викликаємо функцію, яка звернеться до нашого Spring Boot бекенду
        fetchRoute();
    }
});

// --- 7. ПІДКЛЮЧЕННЯ ДО БЕКЕНДУ (і до ORS через нього) ---

async function fetchRoute() {
    console.log("Відправка точок на бекенд:", waypoints);

    try {
        // Це той самий POST-запит, який ми тестували в Postman!
        const response = await fetch('/api/route/calculate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            // Перетворюємо наш масив JavaScript на JSON-рядок
            body: JSON.stringify(waypoints)
        });

        if (!response.ok) {
            throw new Error(`Помилка HTTP: ${response.status}`);
        }

        // Отримуємо JSON-відповідь від нашого Spring Service
        const routeData = await response.json();

        // routeData - це наш DetailedRoute.java, перетворений на JSON
        console.log("Отримано відповідь від бекенду:", routeData);

        // **НАСТУПНИЙ КРОК:**
        // Зараз 'routeData.trackPoints' ще порожній.
        // Коли ми реалізуємо парсинг ORS, тут буде масив координат.

        // if (routeData.trackPoints && routeData.trackPoints.length > 0) {
        //     // (Цей код поки не спрацює, але він потрібен для майбутнього)
        //     // Очищаємо старий маршрут
        //     if (routeLayer) {
        //         map.removeLayer(routeLayer);
        //     }
        //     // Малюємо новий
        //     const latLngs = routeData.trackPoints.map(p => [p.latitude, p.longitude]);
        //     routeLayer = L.polyline(latLngs, { color: 'blue' }).addTo(map);
        // }

    } catch (error) {
        console.error("Не вдалося отримати маршрут:", error);
    }
}