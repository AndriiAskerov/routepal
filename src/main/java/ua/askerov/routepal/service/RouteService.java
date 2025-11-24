package ua.askerov.routepal.service;

import ua.askerov.routepal.model.RouteResponseDTO;
import ua.askerov.routepal.model.Waypoint;

import java.util.List;

public interface RouteService {
    // Метод, який буде взаємодіяти з зовнішнім API
    RouteResponseDTO calculateRoute(List<Waypoint> waypoints);
}