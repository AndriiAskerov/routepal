package ua.askerov.routepal.model;

import java.util.List;

public class ElevationResponseDTO {
    private List<Waypoint> trackPoints;
    private List<ClimbDTO> climbs;
    private double totalAscent;

    public ElevationResponseDTO(List<Waypoint> trackPoints, List<ClimbDTO> climbs, double totalAscent) {
        this.trackPoints = trackPoints;
        this.climbs = climbs;
        this.totalAscent = totalAscent;
    }

    public List<Waypoint> getTrackPoints() { return trackPoints; }
    public List<ClimbDTO> getClimbs() { return climbs; }
    public double getTotalAscent() { return totalAscent; }
}