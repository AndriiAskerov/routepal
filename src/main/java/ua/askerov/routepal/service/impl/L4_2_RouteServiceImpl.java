package ua.askerov.routepal.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.RouteService;

import java.util.List;

@Service
public class L4_2_RouteServiceImpl implements RouteService {

    // 1. ПОЛЯ КЛАСУ (раніше відсутні)
    private final WebClient webClient;
    private final String orsProfile; // Змінна для профілю (cycling-road)

    // 2. КОНСТРУКТOR (Ініціалізує поля)
    // Spring Boot автоматично викличе цей конструктор
    public L4_2_RouteServiceImpl(@Value("${ors.api.key}") String orsApiKey, @Value("${ors.api.url}") String orsUrl, @Value("${ors.profile}") String orsProfile // <-- Впорскуємо профіль
    ) {
        this.orsProfile = orsProfile; // Зберігаємо профіль

        // 3. ІНІЦІАЛІЗАЦІЯ WEBCLIENT
        // Створюємо клієнт один раз при запуску
        this.webClient = WebClient.builder().baseUrl(orsUrl) // Базовий URL (https://api.openrouteservice.org/v2/directions/)
                .defaultHeader("Authorization", orsApiKey) // API ключ для всіх запитів
                .defaultHeader("Accept", "application/json").defaultHeader("Content-Type", "application/json").build();
    }

    @Override
    public RouteResponseDTO calculateRoute(List<Waypoint> waypoints) {
        // Інформуємо про дії консоль
        System.out.println(this.getClass().getSimpleName() + ": Отримано запит! К-ть точок: " + waypoints.size());
        // Повертаємо просту відповідь
        return new RouteResponseDTO("OK", this.getClass().getSimpleName() + ": Маршрут побудовано!");
    }
}