/**
 * cache.js
 * Відповідає за зберігання вже розрахованих маршрутів.
 */
const routeCache = new Map();

/**
 * Генерує унікальний ключ для списку точок.
 * Використовує координати з точністю до 5 знаків (щоб мікро-зсуви не ламали кеш).
 */
export function generateKey(waypoints) {
    return waypoints
        .map(p => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`)
        .join('|'); // "50.123,30.123|50.456,30.456"
}

export function get(waypoints) {
    const key = generateKey(waypoints);
    if (routeCache.has(key)) {
        console.log(`[Cache] Hit: ${key}`);
        return routeCache.get(key);
    }
    return null;
}

export function set(waypoints, data) {
    const key = generateKey(waypoints);
    // Можна додати обмеження розміру кешу (наприклад, зберігати тільки останні 10)
    if (routeCache.size > 20) {
        const firstKey = routeCache.keys().next().value;
        routeCache.delete(firstKey);
    }
    routeCache.set(key, data);
}

export function clear() {
    routeCache.clear();
}