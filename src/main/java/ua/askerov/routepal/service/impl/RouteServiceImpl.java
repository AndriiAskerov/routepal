package ua.askerov.routepal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.RouteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RouteServiceImpl implements RouteService {

    private final String orsProfile;
    private final WebClient webClient;

    public RouteServiceImpl(OrsConfigProperties orsConfig) {
        this.orsProfile = orsConfig.getProfile();

        this.webClient = WebClient.builder()
                .baseUrl(orsConfig.getApi().getUrl())
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Accept", "application/json, application/geo+json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public RouteResponseDTO calculateRoute(List<Waypoint> waypoints) {
        System.out.println(this.getClass().getSimpleName() + ": запит! К-ть точок: " + waypoints.size()); // dbg прибрати вивід todo логування

        if (waypoints == null || waypoints.size() < 2) {
            return RouteResponseDTO.builder()
                    .status("error")
                    .message(this.getClass().getSimpleName() + "Для побудови маршруту потрібно мінімум 2 точки").build();
        }

        // 1. Підготовка координат для ORS.
        // порядок [longitude, latitude]
        List<double[]> coordinates = waypoints.stream()
                .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                .toList();

        // Тіло запиту JSON
        Map<String, Object> requestBody = Map.of("coordinates", coordinates);

        try {
            // 2. Виклик API ORS
            // Формуємо URL: /v2/directions/{profile}/geojson
            JsonNode responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/directions/" + orsProfile + "/geojson").build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(); // Блокуємо потік, оскільки контролер синхронний

            // 3. Обробка відповіді (Парсинг GeoJSON)
            if (responseJson == null || !responseJson.has("features")) {
                return RouteResponseDTO.builder()
                        .status("error")
                        .message(this.getClass().getSimpleName() + "Порожня або некоректна відповідь від ORS API").build();
            }

            // Отримуємо перший елемент features (основний маршрут)
            JsonNode feature = responseJson.path("features").get(0);
            JsonNode properties = feature.path("properties");
            JsonNode summary = properties.path("summary");
            JsonNode geometry = feature.path("geometry");
            JsonNode coordsNode = geometry.path("coordinates");

            // Отримуємо статистику
            double distsance = summary.path("distance").asDouble(); // в метрах
            long duration = summary.path("duration").asLong();      // в секундах

            // Перетворюємо координати маршруту назад у список Waypoint
            List<Waypoint> trackPoints = new ArrayList<>();
            if (coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    // GeoJSON повертає [lon, lat, elevation(optional)]
                    double lon = point.get(0).asDouble();
                    double lat = point.get(1).asDouble();
                    // Створюємо точку (elevation ставимо 0 або зчитуємо, якщо є)
                    trackPoints.add(new Waypoint(lat, lon, 0.0));
                }
            }

            System.out.printf("Маршрут побудовано: %.2f м, %d с, точок графіка: %d%n", distsance, duration, trackPoints.size()); // dbg прибрати вивід todo логування

            // 4. Формування успішної відповіді
            return RouteResponseDTO.builder()
                    .status("success")
                    .message(this.getClass().getSimpleName() + "Маршрут успішно побудовано!")
                    .trackPoints(trackPoints)
                    .distanceMeters(distsance)
                    .durationSeconds(duration).build();

        } catch (Exception e) {
            e.printStackTrace();
            return RouteResponseDTO.builder()
                    .status("error")
                    .message(this.getClass().getSimpleName() + "Помилка при розрахунку маршруту: " + e.getMessage()).build();
        }
    }
}