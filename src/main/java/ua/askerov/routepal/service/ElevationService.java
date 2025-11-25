package ua.askerov.routepal.service;

import ua.askerov.routepal.model.ElevationResponseDTO;
import ua.askerov.routepal.model.Waypoint;

import java.util.List;

public interface ElevationService {
    ElevationResponseDTO getElevationForTrack(List<Waypoint> trackPoints);
}