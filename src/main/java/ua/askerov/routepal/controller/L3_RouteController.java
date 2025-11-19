package ua.askerov.routepal.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.impl.L4_2_RouteServiceImpl_Mock;

import java.util.List;

@RestController
@RequestMapping("/api/route")
public class L3_RouteController {

    private final L4_2_RouteServiceImpl_Mock routeService; // Ін'єкція сервісу

    public L3_RouteController(L4_2_RouteServiceImpl_Mock routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/calculate")
    public RouteResponseDTO calculateRoute(@RequestBody List<Waypoint> waypoints) {
        // Контролер просто передає виклик сервісу і повертає його результат
        return routeService.calculateRoute(waypoints);
    }

}