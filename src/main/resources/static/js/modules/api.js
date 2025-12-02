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
    if (!response.ok) throw new Error('Помилка отримання маршруту');
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
 * ... todo ОНОВИТИ ДОКУМЕНТАЦІЮ МЕТОДУ
 * @returns {Promise<String>} ID зашареного маршруту.
 */
export async function shareRouteApi(requestData) {
    const response = await fetch('/api/route/share', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData) // <-- Відправляємо весь DTO
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Share failed');
    }

    const data = await response.json();
    return data.id;
}

/**
 * Виконує запит на експорт файлу.
 * @param {Object} requestDTO - ExportRequestDTO на Java.
 */
export async function exportRouteApi(requestDTO) {
    const response = await fetch('/api/route/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestDTO) // Просто перетворюємо об'єкт у текст
    });

    if (!response.ok) throw new Error('Export failed');

    let filename = 'route.gpx';
    const disposition = response.headers.get('Content-Disposition');
    if (disposition && disposition.indexOf('attachment') !== -1) {
        const filenameRegex = /filename\*=UTF-8''([^;]+)|filename="([^"]+)"/;
        const matches = filenameRegex.exec(disposition);
        if (matches) {
            if (matches[1]) filename = decodeURIComponent(matches[1]);
            else if (matches[2]) filename = matches[2];
        }
    }

    const blob = await response.blob();
    return { blob, filename };
}