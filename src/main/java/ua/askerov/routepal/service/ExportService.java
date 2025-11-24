package ua.askerov.routepal.service;

import ua.askerov.routepal.model.Waypoint;

import java.util.List;

public interface ExportService {
    /**
     * Експортує маршрут у формат GPX (XML String).
     */
    String exportRoute(List<Waypoint> waypoints);
}