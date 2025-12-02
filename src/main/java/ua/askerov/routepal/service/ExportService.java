package ua.askerov.routepal.service;

import ua.askerov.routepal.model.ExportRequestDTO;
import ua.askerov.routepal.model.Waypoint;

import java.util.List;

public interface ExportService {
    /**
     * Експортує маршрут у формат GPX (XML String).
     */
    String exportRoute(ExportRequestDTO exportRequestDTO);
}