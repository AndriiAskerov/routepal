package ua.askerov.routepal.service;

import ua.askerov.routepal.model.Waypoint;

import java.util.List;

public interface ElevationService {
    List<Waypoint> getElevationForTrack(List<Waypoint> trackPoints);
}