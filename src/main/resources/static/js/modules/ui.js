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

// Функції для керування виглядом панелі висот
export function setElevationPanelState(isOpen) {
    const panel = document.getElementById('elevation-panel');
    const chevron = document.getElementById('elevation-chevron');

    if (isOpen) {
        panel.classList.add('open');
        chevron.className = 'fas fa-chevron-down';
    } else {
        panel.classList.remove('open', 'expanded'); // Також прибираємо expanded при закритті
        chevron.className = 'fas fa-chevron-up';
    }
}

export function toggleChartExpand() {
    const panel = document.getElementById('elevation-panel');
    panel.classList.toggle('expanded');
    return panel.classList.contains('expanded');
}

/**
 * Прив'язка глобальних кнопок до функцій з main.js
 * HTML використовує onclick="func()", ми експортуємо ці функції в window.
 */
export function bindUiActions({
                                  onAddPointMode,
                                  onReverse,
                                  onExport,
                                  onShareQr,
                                  onCloseQrModal,
                                  onTitleChange,        // <-- Нове: зміна назви
                                  onToggleElevation,    // <-- Нове: клік по заголовку висот
                                  onExpandElevationChart: onExpandElevationChart         // <-- Нове: розгортання графіку
                              }) {
    // Біндинг глобальних кнопок (для onclick в HTML)
    window.addPointMode = onAddPointMode;
    window.reverseRoute = onReverse;
    window.exportRoute = onExport;
    window.shareRouteQr = onShareQr;
    window.closeQrModal = onCloseQrModal;

    // Біндинг нових функцій панелі висот (для onclick в HTML)
    window.toggleElevationPanel = onToggleElevation;
    window.toggleExpandChart = onExpandElevationChart;

    // Біндинг інпуту назви (через addEventListener, бо це зручніше для input)
    const titleInput = document.getElementById('route-title-input');
    if (titleInput) {
        titleInput.addEventListener('input', (e) => onTitleChange(e.target.value));
    }
}

// Функція для керування видимістю САМОЇ ПАНЕЛІ (не розгортання, а display: none/flex)
export function setPanelVisibility(isVisible) {
    const panel = document.getElementById('elevation-panel');
    if (panel) {
        panel.style.display = isVisible ? 'flex' : 'none';
    }
}