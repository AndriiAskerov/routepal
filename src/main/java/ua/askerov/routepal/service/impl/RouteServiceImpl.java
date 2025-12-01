package ua.askerov.routepal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.ElevationResponseDTO; // Не забудьте імпорт
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ElevationService; // Не забудьте імпорт
import ua.askerov.routepal.service.RouteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RouteServiceImpl implements RouteService {

    private final String orsProfile;
    private final WebClient webClient;

    // 1. Додаємо залежність від сервісу висот
    private final ElevationService elevationService;

    // 2. Оновлюємо конструктор
    public RouteServiceImpl(OrsConfigProperties orsConfig, ElevationService elevationService) {
        this.orsProfile = orsConfig.getProfile();
        this.elevationService = elevationService; // Ініціалізація

        this.webClient = WebClient.builder()
                .baseUrl(orsConfig.getApi().getUrl())
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Accept", "application/json, application/geo+json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public RouteResponseDTO calculateRoute(List<Waypoint> waypoints) {
        System.out.println(this.getClass().getSimpleName() + ": запит! К-ть точок: " + waypoints.size());

        if (waypoints == null || waypoints.size() < 2) {
            return RouteResponseDTO.builder()
                    .status("error")
                    .message("Для побудови маршруту потрібно мінімум 2 точки").build();
        }

        List<double[]> coordinates = waypoints.stream()
                .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                .toList();

        Map<String, Object> requestBody = Map.of("coordinates", coordinates);

        try {
            JsonNode responseJson = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/directions/" + orsProfile + "/geojson").build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (responseJson == null || !responseJson.has("features")) {
                return RouteResponseDTO.builder()
                        .status("error")
                        .message("Порожня або некоректна відповідь від ORS API").build();
            }

            JsonNode feature = responseJson.path("features").get(0);
            JsonNode summary = feature.path("properties").path("summary");
            JsonNode coordsNode = feature.path("geometry").path("coordinates");

            double distance = summary.path("distance").asDouble();
            long duration = summary.path("duration").asLong();

            // Парсимо "плоский" маршрут (без реальних висот, ORS часто віддає 0 або неточні дані тут)
            List<Waypoint> flatTrackPoints = new ArrayList<>();
            if (coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    double lon = point.get(0).asDouble();
                    double lat = point.get(1).asDouble();
                    flatTrackPoints.add(new Waypoint(lat, lon, 0.0));
                }
            }

            // === 3. ВИКЛИКАЄМО ELEVATION SERVICE ===
            // Ми беремо "плоскі" точки і просимо сервіс знайти для них висоту та підйоми
            ElevationResponseDTO elevationData = elevationService.getElevationForTrack(flatTrackPoints);

            // Використовуємо збагачені точки (з висотою) та список підйомів
            List<Waypoint> enrichedPoints = elevationData.getTrackPoints();
            // Якщо раптом сервіс повернув пустий список (помилка), беремо flatTrackPoints
            if (enrichedPoints == null || enrichedPoints.isEmpty()) {
                enrichedPoints = flatTrackPoints;
            }

            System.out.printf("Маршрут: %.2f м, підйомів знайдено: %d%n", distance,
                    (elevationData.getClimbs() != null ? elevationData.getClimbs().size() : 0));

            // 4. Формуємо відповідь з CLIMBS
            return RouteResponseDTO.builder()
                    .status("success")
                    .message("Маршрут успішно побудовано!")
                    .trackPoints(enrichedPoints) // Тут тепер точки з реальною висотою
                    .distanceMeters(distance)
                    .durationSeconds(duration)
                    .climbs(elevationData.getClimbs()) // <--- ОСЬ ВОНО!
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return RouteResponseDTO.builder()
                    .status("error")
                    .message("Помилка при розрахунку маршруту: " + e.getMessage()).build();
        }
    }
}