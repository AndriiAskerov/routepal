// Встановлюємо початковий вид (поки що, на Київ) TODO: відповідно до розташування клієнта
const map = L.map('map').setView([50.4501, 30.5234], 12);

// Додаємо "плитки" (tiles) карти
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'})
    .addTo(map);

let waypoints = [];
let route = null; // Лінія маршруту

// Обробник ЛКМ на карті
map.on('click', function (e) {
    const newPoint = e.latlng;
    console.log(e); // DBG: прибрати вивід на консоль
    // Додавання маркеру на карту
    L.marker([newPoint.lat, newPoint.lng]).addTo(map);
    // Додавання координат (у форматі)
    waypoints.push({
        latitude: newPoint.lat, longitude: newPoint.lng
    });

    // Якщо точок 2, або більше - будується маршрут
    if (waypoints.length >= 2) {
        // Звернення до Spring Boot бекенду
        fetchRoute();
    }
});

async function fetchRoute() {
    console.log("Відправка точок на бекенд:", waypoints);

    try {
        const response = await fetch('/api/route/calculate', {
            method: 'POST', headers: {
                'Content-Type': 'application/json'
            }, body: JSON.stringify(waypoints)
        });

        if (!response.ok) {
            // 1. Якщо статус не OK (наприклад, 429), треба прочитати JSON-тіло помилки
            const errorData = await response.json().catch(() => null);

            // 2. Якщо сервер передав повідомлення (message), покажемо його
            if (errorData && errorData.message) {
                // Вікно, що спливає, з текстом у ньому
                alert("Увага: " + errorData.message);
                console.error("Помилка сервера:", errorData.message);
            } else {
                alert(`Сталася помилка HTTP: ${response.status}`);
            }
            // Зупинка виконання функції
            return;
        }

        const routeData = await response.json();

        console.log("Отримано відповідь від бекенду:", routeData);

        // --- НОВА ЛОГІКА МАЛЮВАННЯ ---

        // 1. Очищаємо попередній маршрут (якщо він був)
        if (route) {
            map.removeLayer(route);
        }

        // 2. Перевіряємо, чи бекенд повернув точки
        if (routeData.status === 'success' && routeData.trackPoints && routeData.trackPoints.length > 0) {

            // 3. Конвертуємо наш список точок у формат, який розуміє Leaflet: [ [lat, lon], [lat, lon], ... ]
            const latLngs = routeData.trackPoints.map(point => {
                // Ми отримуємо {latitude: ..., longitude: ..., elevation: ...}
                return [point.latitude, point.longitude];
            });

            // 4. Створюємо лінію (Polyline) і додаємо її на карту
            route = L.polyline(latLngs, {color: 'blue', weight: 5}).addTo(map);

            // (Опціонально) Фокусуємо карту на новому маршруті
            map.fitBounds(route.getBounds());
        }
        // --- КІНЕЦЬ НОВОЇ ЛОГІКИ ---

    } catch (error) {
        console.error("Не вдалося отримати маршрут:", error);
    }
}