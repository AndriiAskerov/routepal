package ua.askerov.routepal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.impl.ExportServiceImpl;
import ua.askerov.routepal.service.impl.RouteServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteServiceImpl routeService; // Ін'єкція сервісу
    private final ExportServiceImpl exportService;

    // Тимчасове сховище маршрутів: ID -> Список точок
    // У реальному проді тут потрібен Redis або БД з автовидаленням, але для тесту Map підійде.
    private final Map<String, List<Waypoint>> sharedRoutes = new ConcurrentHashMap<>();


    public RouteController(RouteServiceImpl routeService, ExportServiceImpl exportService) {
        this.routeService = routeService;
        this.exportService = exportService;
    }

    @PostMapping("/calculate")
    public RouteResponseDTO calculateRoute(@RequestBody List<Waypoint> waypoints) {
        // Контролер просто передає виклик сервісу і повертає його результат
        return routeService.calculateRoute(waypoints);
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportRoute(@RequestBody List<Waypoint> waypoints) {
        try {
            // Викликаємо спеціалізований сервіс

            String gpxXml = exportService.exportRoute(waypoints);

            // Повертаємо файл
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"route.gpx\"")
                    .contentType(MediaType.APPLICATION_XML)
                    .body(gpxXml);

        } catch (RuntimeException e) {
            // Обробка помилок (наприклад, якщо ліміт вичерпано і Аспект кинув виняток)
            if ("LIMIT_EXCEEDED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new RouteResponseDTO.Builder()
                                .status("error")
                                .message("Денний ліміт експорту вичерпано.")
                                .build());
            }
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // --- НОВІ МЕТОДИ ДЛЯ QR ---

    // 1. Зберегти маршрут і отримати ID
    @PostMapping("/share")
    public ResponseEntity<Map<String, String>> shareRoute(@RequestBody List<Waypoint> waypoints) {
        String id = UUID.randomUUID().toString();
        sharedRoutes.put(id, waypoints);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // 2. Завантажити файл за ID (GET запит для мобільного)
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadSharedRoute(@PathVariable String id) {
        List<Waypoint> waypoints = sharedRoutes.get(id);

        if (waypoints == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Маршрут не знайдено або термін дії посилання вичерпано.");
        }

        try {
            String gpxXml = exportService.exportRoute(waypoints);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"route_shared.gpx\"")
                    .contentType(MediaType.APPLICATION_XML)
                    .body(gpxXml);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating GPX");
        }
    }

}