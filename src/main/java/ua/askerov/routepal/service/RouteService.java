package ua.askerov.routepal.service;

import ua.askerov.routepal.model.DetailedRoute;
import ua.askerov.routepal.model.Waypoint;

import java.util.List;

// Інтерфейс визначає, що повинен робити наш сервіс
public interface RouteService {
    // Метод, який буде взаємодіяти з зовнішнім API
    DetailedRoute calculateAndFetchRoute(List<Waypoint> waypoints);
}