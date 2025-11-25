package ua.askerov.routepal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.ClimbDTO;
import ua.askerov.routepal.model.ElevationResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ElevationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElevationServiceImpl implements ElevationService { // Перейменуйте клас, якщо він у вас ElevationServiceImpl

    private final WebClient webClient;

    // Параметри для визначення підйому
    private static final double MIN_GRADIENT = 3.0; // Мінімальний нахил 3%
    private static final double MIN_DISTANCE = 300; // Мінімальна довжина 300м
    private static final double MIN_ELEVATION_GAIN = 15; // Мінімальний набір 15м

    public ElevationServiceImpl(OrsConfigProperties orsConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openrouteservice.org")
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // Змінюємо тип повернення на DTO
    @Override
    public ElevationResponseDTO getElevationForTrack(List<Waypoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) {
            return new ElevationResponseDTO(new ArrayList<>(), new ArrayList<>());
        }

        // 1. Отримуємо висоти від API (Ваш існуючий код)
        List<double[]> coordinates = trackPoints.stream()
                .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                .toList();

        Map<String, Object> requestBody = Map.of(
                "format_in", "polyline",
                "geometry", coordinates
        );

        List<Waypoint> enrichedPoints = new ArrayList<>();
        try {
            JsonNode response = webClient.post()
                    .uri("/elevation/line")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode coordsNode = response.path("geometry").path("coordinates");
            if (coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    double lon = point.get(0).asDouble();
                    double lat = point.get(1).asDouble();
                    double ele = point.get(2).asDouble();
                    enrichedPoints.add(new Waypoint(lat, lon, ele));
                }
            }
        } catch (Exception e) {
            System.err.println("Elevation API Error: " + e.getMessage());
            // У разі помилки повертаємо точки без висоти (або з 0)
            enrichedPoints = new ArrayList<>(trackPoints);
        }

        // 2. АНАЛІЗ ПІДЙОМІВ
        List<ClimbDTO> climbs = detectClimbs(enrichedPoints);

        return new ElevationResponseDTO(enrichedPoints, climbs);
    }

    private List<ClimbDTO> detectClimbs(List<Waypoint> points) {
        List<ClimbDTO> climbs = new ArrayList<>();
        if (points.size() < 2) return climbs;

        int startIndex = -1;
        double segmentDist = 0;
        double startEle = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            Waypoint p1 = points.get(i);
            Waypoint p2 = points.get(i+1);

            double dist = distance(p1, p2);
            double eleDiff = p2.getElevation() - p1.getElevation();

            // Якщо дистанція 0 (дублікати точок), пропускаємо
            if (dist == 0) continue;

            double gradient = (eleDiff / dist) * 100;

            // Логіка початку/продовження підйому
            if (gradient >= MIN_GRADIENT) {
                if (startIndex == -1) {
                    startIndex = i;
                    startEle = p1.getElevation();
                    segmentDist = 0;
                }
                segmentDist += dist;
            } else {
                // Градієнт впав. Перевіряємо, чи це кінець підйому, чи короткий "виполог"
                // Для простоти: якщо градієнт став < 3%, закриваємо сегмент.
                // (У складніших алгоритмах можна дозволяти короткі спади)

                if (startIndex != -1) {
                    finishSegment(climbs, points, startIndex, i, segmentDist, startEle);
                    startIndex = -1; // Скидаємо
                }
            }
        }

        // Перевірка останнього сегменту, якщо він йшов до самого фінішу
        if (startIndex != -1) {
            finishSegment(climbs, points, startIndex, points.size() - 1, segmentDist, startEle);
        }

        return climbs;
    }

    private void finishSegment(List<ClimbDTO> climbs, List<Waypoint> points, int start, int end, double dist, double startEle) {
        double endEle = points.get(end).getElevation();
        double gain = endEle - startEle;

        // Фільтрація шумів:
        if (dist >= MIN_DISTANCE && gain >= MIN_ELEVATION_GAIN) {
            double avgGrad = (gain / dist) * 100;
            climbs.add(new ClimbDTO(start, end, dist, avgGrad, gain));
        }
    }

    // Haversine formula (у метрах)
    private double distance(Waypoint p1, Waypoint p2) {
        double R = 6371e3;
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}