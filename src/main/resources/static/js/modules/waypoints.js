import * as MapCore from './map-core.js';

/**
 * waypoints.js
 * Керує списком точок: додавання, видалення, рендеринг у сайдбарі.
 */

const listElement = document.getElementById('waypoints-list');

/**
 * Створює нову точку, додає маркер і повертає об'єкт точки.
 */
export function createWaypoint(id, lat, lng, index, callbacks) {
    // 1. Створюємо маркер через MapCore
    const marker = MapCore.addMarkerToMap(
        lat, lng,
        index,
        (newLat, newLng) => callbacks.onDrag(id, newLat, newLng), // DragEnd
        () => callbacks.onRemove(id) // RightClick
    );

    return {
        id: id,
        lat: lat,
        lng: lng,
        marker: marker,
        name: '' // Назва за замовчуванням вираховується при рендері
    };
}

/**
 * Оновлює весь HTML список у сайдбарі.
 */
export function renderSidebar(waypoints, onRemoveClick, onNameChange) {
    listElement.innerHTML = '';

    waypoints.forEach((wp, index) => {
        const el = document.createElement('div');
        el.className = 'waypoint-item';
        el.dataset.id = wp.id; // Потрібно для SortableJS

        // Визначаємо дефолтну назву
        let placeholder = 'Точка';
        if (index === 0) placeholder = 'Старт';
        else if (index === waypoints.length - 1) placeholder = 'Фініш';

        el.innerHTML = `
            <i class="fas fa-bars handle" style="cursor: grab; color:#aaa; margin-right:8px; padding: 5px;"></i>
            <span class="waypoint-number">${index + 1}.</span>
            <input type="text" class="waypoint-name-input" 
                   value="${wp.name}" placeholder="${placeholder}">
            <button class="btn-delete"><i class="fas fa-trash-alt"></i></button>
        `;

        // Події
        const input = el.querySelector('.waypoint-name-input');
        input.addEventListener('input', (e) => onNameChange(wp.id, e.target.value));

        const deleteBtn = el.querySelector('.btn-delete');
        deleteBtn.addEventListener('click', () => onRemoveClick(wp.id));

        listElement.appendChild(el);
    });
}

/**
 * Оновлює іконки на карті (цифри 1, 2, 3...) після зміни порядку.
 */
export function refreshMarkers(waypoints) {
    waypoints.forEach((wp, idx) => {
        MapCore.updateMarkerIcon(wp.marker, idx + 1);
    });
}

/**
 * Ініціалізує Drag & Drop.
 */
export function initSortable(onReorder) {
    Sortable.create(listElement, {
        handle: '.handle',
        animation: 150,
        ghostClass: 'sortable-placeholder',
        onEnd: (evt) => {
            if (evt.oldIndex !== evt.newIndex) {
                onReorder(evt.oldIndex, evt.newIndex);
            }
        }
    });
}