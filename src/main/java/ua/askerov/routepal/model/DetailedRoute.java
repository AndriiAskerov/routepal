package ua.askerov.routepal.model;

import java.util.Collections;
import java.util.List;

// Це заглушка, яку ви будете розширювати, щоб зберігати реальний трек від ORS
public class DetailedRoute {
    private final String message = "Маршрут розраховано, готовий до відображення.";
    private final List<Waypoint> trackPoints = Collections.emptyList(); // Тут будуть точки треку з висотами
    private String status = "processing";

    // **ОБОВ'ЯЗКОВО ДЛЯ JSON-СЕРІАЛІЗАЦІЇ (ВИХІДНИХ ДАНИХ):**
    public String getStatus() {
        return status;
    }

    // Сеттери потрібні, якщо ви створюєте об'єкт і пізніше встановлюєте поля
    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public List<Waypoint> getTrackPoints() {
        return trackPoints;
    }

    // ... інші сеттери
}