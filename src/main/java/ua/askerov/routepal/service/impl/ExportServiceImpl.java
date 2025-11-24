package ua.askerov.routepal.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ExportService;

import java.util.List;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    private final WebClient webClient;
    private final String orsProfile;

    // Ми можемо перевикористати ті самі налаштування ORS,
    // або додати окремі (наприклад, ors.export.url), якщо провайдер зміниться.
    public ExportServiceImpl(OrsConfigProperties orsConfig) {
        this.orsProfile = orsConfig.getProfile();

        this.webClient = WebClient.builder()
                .baseUrl(orsConfig.getApi().getUrl())
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .build();
    }

    @Override
    public String exportRoute(List<Waypoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("Для експорту потрібно мінімум 2 точки");
        }

        // 1. Конвертація точок [lon, lat]
        List<double[]> coordinates = waypoints.stream()
                .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                .toList();

        Map<String, Object> requestBody = Map.of("coordinates", coordinates);

        try {
            // 2. Запит до ORS GPX endpoint
            String rawGpx = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/directions/" + orsProfile + "/gpx").build())
                    .bodyValue(requestBody)
                    .header("Accept", "application/gpx+xml")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 3. !!! ТУТ МАГІЯ: Виправляємо файл для Garmin/Strava перед поверненням !!!
            return fixGpxForGarmin(rawGpx);

        } catch (Exception e) {
            // Логування помилки
            System.err.println("Помилка ExportService: " + e.getMessage());
            throw new RuntimeException("Не вдалося згенерувати GPX файл: " + e.getMessage());
        }
    }

    /**
     * Метод для виправлення GPX від OpenRouteService, щоб його розумів Garmin.
     * 1. Замінює namespace на стандартний.
     * 2. Конвертує Route (rte) у Track (trk).
     */
    private String fixGpxForGarmin(String orsGpx) {
        if (orsGpx == null) return null;

        // Крок 1: Заміна заголовка (Namespace) на стандартний Topografix
        // Garmin ігнорує файли з нестандартними xmlns
        String fixedGpx = orsGpx.replace(
                "https://raw.githubusercontent.com/GIScience/openrouteservice-schema/main/gpx/v2/ors-gpx.xsd",
                "http://www.topografix.com/GPX/1/1"
        );

        // Додаємо стандартні атрибути схеми, якщо їх немає (для повної валідності)
        if (!fixedGpx.contains("xsi:schemaLocation")) {
            fixedGpx = fixedGpx.replace(
                    "<gpx ",
                    "<gpx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                            "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\" "
            );
        }

        // Крок 2: Конвертація Route (<rte>) у Track (<trk>)
        // Це потрібно, щоб велокомп'ютер вів по лінії ("ниточці"), а не просто показував точки повороту.

        // Замінюємо відкриваючий тег маршруту на трек + сегмент
        fixedGpx = fixedGpx.replace("<rte>", "<trk><name>My Route</name><trkseg>");

        // Замінюємо закриваючий тег
        fixedGpx = fixedGpx.replace("</rte>", "</trkseg></trk>");

        // Замінюємо точки маршруту (rtept) на точки треку (trkpt)
        fixedGpx = fixedGpx.replace("<rtept", "<trkpt");
        fixedGpx = fixedGpx.replace("</rtept>", "</trkpt>");

        return fixedGpx;
    }
}