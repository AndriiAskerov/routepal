package ua.askerov.routepal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ElevationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElevationServiceImpl implements ElevationService {

    private final WebClient webClient;

    public ElevationServiceImpl(OrsConfigProperties orsConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openrouteservice.org") // Базовий URL для всіх сервісів
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public List<Waypoint> getElevationForTrack(List<Waypoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) return new ArrayList<>();

        // 1. Формуємо список координат [[lon, lat], [lon, lat], ...]
        List<double[]> coordinates = trackPoints.stream()
                .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                .toList();

        // 2. ВИПРАВЛЕНО: Використовуємо "format_in": "polyline"
        // API очікує просто масив точок у полі "geometry", якщо вказано "polyline"
        Map<String, Object> requestBody = Map.of(
                "format_in", "polyline",
                "geometry", coordinates
        );

        try {
            JsonNode response = webClient.post()
                    .uri("/elevation/line")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // 3. Парсимо відповідь.
            // При format_in=polyline, ORS зазвичай повертає GeoJSON у відповідь (за замовчуванням)
            // Структура: { "geometry": { "coordinates": [ [lon, lat, ele], ... ] } }
            List<Waypoint> resultPoints = new ArrayList<>();
            JsonNode coordsNode = response.path("geometry").path("coordinates");

            if (coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    double lon = point.get(0).asDouble();
                    double lat = point.get(1).asDouble();
                    double ele = point.get(2).asDouble(); // Висота
                    resultPoints.add(new Waypoint(lat, lon, ele));
                }
            }
            return resultPoints;

        } catch (Exception e) {
            // Для дебагу краще вивести тіло помилки, якщо це WebClientResponseException
            System.err.println("Помилка Elevation API: " + e.getMessage());
            throw new RuntimeException("Не вдалося отримати дані висот: " + e.getMessage());
        }
    }
}