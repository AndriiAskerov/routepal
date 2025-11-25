/**
 * api.js
 * Відповідає за всі мережеві запити до бекенду (RoutePal API).
 */

/**
 * Відправляє запит на розрахунок маршруту.
 * @param {Array} waypoints - Масив об'єктів точок {lat, lng}.
 * @returns {Promise<Object>} JSON відповідь з маршрутом.
 */
export async function calculateRouteApi(waypoints) {
    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));
    const response = await fetch('/api/route/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.message || 'Помилка розрахунку маршруту');
    }
    return await response.json();
}

/**
 * Отримує дані висот для заданого треку.
 * @param {Array} trackPoints - Повний список точок з геометрії маршруту.
 * @returns {Promise<Array>} Масив точок з висотами.
 */
export async function getElevationApi(trackPoints) {
    const response = await fetch('/api/route/elevation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(trackPoints)
    });

    if (!response.ok) throw new Error('Помилка отримання висот');
    return await response.json();
}

/**
 * Генерує QR-код посилання (отримує ID маршруту).
 * @param {Array} waypoints
 * @returns {Promise<String>} ID зашареного маршруту.
 */
export async function shareRouteApi(waypoints) {
    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));
    const response = await fetch('/api/route/share', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error('Помилка Share API');
    const data = await response.json();
    return data.id;
}

/**
 * Виконує запит на експорт файлу.
 * @param {Array} waypoints
 * @returns {Promise<Blob>} Файл (GPX).
 */
export async function exportRouteApi(waypoints) {
    const payload = waypoints.map(p => ({ latitude: p.lat, longitude: p.lng }));
    const response = await fetch('/api/route/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error('Помилка експорту');
    return await response.blob();
}