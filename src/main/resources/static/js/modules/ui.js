/**
 * ui.js
 * Відповідає за загальні елементи інтерфейсу та біндинг кнопок.
 */

// Оновлює блок з дистанцією та часом
export function updateRouteInfo(meters, seconds) {
    const infoDiv = document.getElementById('route-info');

    if (meters === 0) {
        infoDiv.innerHTML = '';
        return;
    }

    const km = (meters / 1000).toFixed(2);
    const totalMin = Math.round(seconds / 60);
    const hours = Math.floor(totalMin / 60);
    const minutes = totalMin % 60;

    let timeStr = `${minutes} хв`;
    if (hours > 0) {
        timeStr = `${hours} год ${minutes} хв`;
    }

    infoDiv.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <span><i class="fas fa-ruler-horizontal"></i> <b>${km} км</b></span>
            <span><i class="far fa-clock"></i> <b>${timeStr}</b></span>
        </div>
    `;
}

/**
 * Прив'язка глобальних кнопок до функцій з main.js
 * Оскільки HTML використовує onclick="func()", ми експортуємо ці функції в window.
 */
export function bindButtons({ onAddPointMode, onReverse, onExport, onShareQr, onCloseQrModal }) {
    window.addPointMode = onAddPointMode;
    window.reverseRoute = onReverse;
    window.exportRoute = onExport;
    window.shareRouteQr = onShareQr;       // <--- Експорт в глобальну область
    window.closeQrModal = onCloseQrModal;  // <--- Експорт в глобальну область
}