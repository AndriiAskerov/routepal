package ua.askerov.routepal.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ua.askerov.routepal.model.DetailedRoute;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.RouteService;

import java.util.List;
import java.util.Map;

// --- Головний Контролер ---
@RestController
public class RouteController {

    // 1. Ін'єкція залежностей (Dependency Injection)
    // Spring автоматично знайде клас, позначений @Service (RouteServiceImpl)
    // і створить його об'єкт (Singleton), який буде доступний тут.
    private final RouteService routeService;

    // Конструктор для ін'єкції залежностей (краща практика)
    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/api/route/calculate")
    // Змінюємо тип повернення на Map (Spring знає, як перетворити Map на JSON)
    public Map<String, Object> calculateRoute(@RequestBody List<Waypoint> waypoints) {

        // Тут ми ігноруємо Waypoints і Service

        System.out.println("ПЕРЕВІРКА: Контролер отримав запит. Точок: " + waypoints.size());

        // Повертаємо простий об'єкт Map, який гарантовано серіалізується в JSON
        return Map.of(
                "testStatus", "OK",
                "message", "Це тестова відповідь без виклику Service."
        );
    }
}