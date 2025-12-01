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
public class ElevationServiceImpl implements ElevationService {

    private final WebClient webClient;

    // === КОНСТАНТИ ДЛЯ НАЛАШТУВАННЯ ЧУТЛИВОСТІ ===
    private static final double MIN_GRADIENT = 3.0;       // Мін. градієнт для початку (%)
    private static final double KEEP_GRADIENT = 1.0;      // Мін. градієнт для продовження (%)
    private static final double MIN_DISTANCE = 300;       // Мін. дистанція для "класичного" підйому (м)
    private static final double WALL_CLIMB_SCORE = 1000;  // Поріг балів для коротких "стінок" (Score = Dist * Grad)

    // Пороги категорій (Strava-like)
    private static final double CAT_4_SCORE = 8000;
    private static final double HARD_SCORE = 3500;

    public ElevationServiceImpl(OrsConfigProperties orsConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openrouteservice.org")
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ElevationResponseDTO getElevationForTrack(List<Waypoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) {
            return new ElevationResponseDTO(new ArrayList<>(), new ArrayList<>());
        }

        // 1. Отримуємо висоти від API
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

            if (response != null) {
                JsonNode coordsNode = response.path("geometry").path("coordinates");
                if (coordsNode.isArray()) {
                    for (JsonNode point : coordsNode) {
                        double lon = point.get(0).asDouble();
                        double lat = point.get(1).asDouble();
                        double ele = point.get(2).asDouble();
                        enrichedPoints.add(new Waypoint(lat, lon, ele));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Elevation API Error: " + e.getMessage());
            enrichedPoints = new ArrayList<>(trackPoints);
        }

        // Якщо API не повернуло точок (або помилка), використовуємо вхідні, але без аналізу підйомів
        if (enrichedPoints.isEmpty()) {
            return new ElevationResponseDTO(trackPoints, new ArrayList<>());
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

            if (dist == 0) continue;

            double gradient = (eleDiff / dist) * 100;

            // Логіка "гістерезису":
            // Починаємо, якщо круто (3%). Продовжуємо, поки не стане зовсім плоско (< 1%).
            if (gradient >= KEEP_GRADIENT) {
                if (startIndex == -1) {
                    // Старт сегменту
                    if (gradient >= MIN_GRADIENT) {
                        startIndex = i;
                        startEle = p1.getElevation();
                        segmentDist = 0;
                    }
                }
                // Продовження сегменту
                if (startIndex != -1) {
                    segmentDist += dist;
                }
            } else {
                // Градієнт впав. Закриваємо сегмент.
                if (startIndex != -1) {
                    validateAndAddClimb(climbs, points, startIndex, i, segmentDist, startEle);
                    startIndex = -1;
                }
            }
        }

        // Перевірка хвоста (якщо маршрут закінчується на горі)
        if (startIndex != -1) {
            validateAndAddClimb(climbs, points, startIndex, points.size() - 1, segmentDist, startEle);
        }

        return climbs;
    }

    private void validateAndAddClimb(List<ClimbDTO> climbs, List<Waypoint> points, int startIndex, int endIndex, double distance, double startEle) {
        double endEle = points.get(endIndex).getElevation();
        double elevationGain = endEle - startEle;

        // Базовий захист
        if (distance == 0 || elevationGain <= 0) return;

        double avgGradient = (elevationGain / distance) * 100;

        // === ОБРАХУНОК "SCORE" ===
        // Формула: Дистанція (м) * Градієнт (%)
        double climbScore = distance * avgGradient;

        // Умова 1: Класичний підйом (довгий)
        boolean isClassicClimb = distance >= MIN_DISTANCE && avgGradient >= MIN_GRADIENT;

        // Умова 2: "Стінка" (коротка, але з великим Score)
        // Наприклад: 150м * 8% = 1200 > 1000 -> Спрацює
        boolean isWall = climbScore >= WALL_CLIMB_SCORE;

        if (isClassicClimb || isWall) {
            ClimbDTO climb = new ClimbDTO();
            climb.setStartIndex(startIndex);
            climb.setEndIndex(endIndex);

            // Зверніть увагу: використовуйте точні назви сеттерів з вашого DTO
            // Якщо у вас setDistanceMeters, то залишаємо так. Якщо просто setDistance - змініть.
            climb.setDistanceMeters(distance); // або climb.setDistance(distance);

            climb.setAvgGradient(avgGradient);
            climb.setElevationGain(elevationGain);

            // === ВИЗНАЧЕННЯ КАТЕГОРІЇ ===
            if (climbScore >= CAT_4_SCORE) {
                climb.setCategory("Cat 4"); // Офіційна категорія UCI/Strava (найлегша з категорійних)
            } else if (climbScore >= HARD_SCORE) {
                climb.setCategory("Тяжкий");
            } else {
                climb.setCategory("Середній");
            }

            climbs.add(climb);
        }
    }

    // Haversine formula (у метрах)
    private double distance(Waypoint p1, Waypoint p2) {
        double R = 6371e3; // Радіус Землі в метрах
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