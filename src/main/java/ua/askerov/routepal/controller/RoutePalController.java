package ua.askerov.routepal.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.askerov.routepal.model.ElevationResponseDTO;
import ua.askerov.routepal.model.ExportRequestDTO;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ElevationService;
import ua.askerov.routepal.service.impl.ExportServiceImpl;
import ua.askerov.routepal.service.impl.RouteServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/route")
public class RoutePalController {

    private final RouteServiceImpl routeService; // Ін'єкція сервісу
    private final ExportServiceImpl exportService;
    private final ElevationService elevationService;

    // Тимчасове сховище маршрутів: ID -> Список точок
    // У реальному проді тут потрібен Redis або БД з автовидаленням, але для тесту Map підійде.
    private final Map<String, ExportRequestDTO> sharedRoutes = new ConcurrentHashMap<>();


    public RoutePalController(RouteServiceImpl routeService, ExportServiceImpl exportService, ElevationService elevationService) {
        this.routeService = routeService;
        this.exportService = exportService;
        this.elevationService = elevationService;
    }

    @PostMapping("/calculate")
    public RouteResponseDTO calculateRoute(@RequestBody List<Waypoint> waypoints) {
        // Контролер просто передає виклик сервісу і повертає його результат
        return routeService.calculateRoute(waypoints);
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportRoute(@RequestBody ExportRequestDTO requestDTO) {
        try {
            String file = exportService.exportRoute(requestDTO);
            String filename = requestDTO.generateFilename();
            // На випадок кирилиці та пробілів
            String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(file);

        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/share")
    public ResponseEntity<Map<String, String>> shareRoute(@RequestBody ExportRequestDTO requestDTO) { // <-- Приймаємо DTO
        String id = UUID.randomUUID().toString();
        sharedRoutes.put(id, requestDTO); // <-- Зберігаємо весь DTO
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadSharedRoute(@PathVariable String id) {
        ExportRequestDTO requestDTO = sharedRoutes.get(id);
        if (requestDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Маршрут не знайдено або термін дії посилання вичерпано.");
        }

        try {
            String file = exportService.exportRoute(requestDTO);
            String filename = requestDTO.generateFilename();
            // На випадок кирилиці та пробілів
            String encodedFilename = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.APPLICATION_XML)
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating GPX");
        }
    }

    @PostMapping("/elevation")
    public ResponseEntity<?> getElevation(@RequestBody List<Waypoint> trackPoints) {
        try {
            ElevationResponseDTO responseDTO = elevationService.getElevationForTrack(trackPoints);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}