/**
 * cache.js
 * Відповідає за зберігання та часткове оновлення даних маршруту.
 */

const storage = new Map();

// Генерує унікальний ключ для маршруту на основі координат точок
function generateKey(waypoints) {
    return JSON.stringify(waypoints.map(p => ({ lat: p.lat, lng: p.lng })));
}

export function get(waypoints) {
    const key = generateKey(waypoints);
    return storage.get(key);
}

// Зберігає базові дані (геометрію)
export function set(waypoints, routeData) {
    const key = generateKey(waypoints);
    // Зберігаємо об'єкт. hasElevation буде false, якщо climbs немає або null
    storage.set(key, {
        ...routeData,
        timestamp: Date.now()
    });
}

// Оновлює існуючий запис (додає висоти)
export function updateWithElevation(waypoints, elevationData) {
    const key = generateKey(waypoints);
    const existing = storage.get(key);

    if (existing) {
        // Ми об'єднуємо старі дані (дистанція, час) з новими (climbs, trackPoints Z)
        const updated = {
            ...existing,
            climbs: elevationData.climbs,
            // Якщо API висот повертає оновлені точки (з Z координатою), беремо їх
            trackPoints: elevationData.trackPoints || existing.trackPoints
        };
        storage.set(key, updated);
        return updated;
    }
    return null;
}

// Допоміжна функція: перевіряє, чи містить кеш дані про висоти
export function hasElevationData(cachedEntry) {
    return cachedEntry && cachedEntry.climbs && cachedEntry.climbs.length > 0;
}