package ua.askerov.routepal.service.impl;

import org.springframework.stereotype.Service;
import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.RouteService;

import java.util.List;

@Service
public class L4_2_RouteServiceImpl_Mock implements RouteService {

    @Override
    public RouteResponseDTO calculateRoute(List<Waypoint> waypoints) {
        // Інформуємо про дії консоль
        System.out.println(this.getClass().getSimpleName() + ": Отримано запит! К-ть точок: " + waypoints.size());
        // Повертаємо просту відповідь
        return new RouteResponseDTO("OK", this.getClass().getSimpleName() + ": Маршрут побудовано!");
    }
}