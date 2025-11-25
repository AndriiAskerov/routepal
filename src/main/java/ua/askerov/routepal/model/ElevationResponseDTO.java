package ua.askerov.routepal.model;

import java.util.List;

public class ElevationResponseDTO {
    private List<Waypoint> trackPoints;
    private List<ClimbDTO> climbs;

    public ElevationResponseDTO(List<Waypoint> trackPoints, List<ClimbDTO> climbs) {
        this.trackPoints = trackPoints;
        this.climbs = climbs;
    }

    public List<Waypoint> getTrackPoints() { return trackPoints; }
    public List<ClimbDTO> getClimbs() { return climbs; }
}