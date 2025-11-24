package ua.askerov.routepal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.impl.ExportServiceImpl;
import ua.askerov.routepal.service.impl.L4_2_RouteServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class L3_RouteController {

    private final L4_2_RouteServiceImpl routeService; // Ін'єкція сервісу
    private final ExportServiceImpl exportService;

    public L3_RouteController(L4_2_RouteServiceImpl routeService, ExportServiceImpl exportService) {
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

}