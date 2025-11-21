package ua.askerov.routepal.model;

import java.util.List;

public class RouteResponseDTO {

    private String status;
    private String message;
    private List<Waypoint> trackPoints;
    private double distanceMeters;
    private long durationSeconds;

    public RouteResponseDTO() {
    }

    public static Builder builder() {
        return new Builder();
    }

    private RouteResponseDTO(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.trackPoints = builder.trackPoints;
        this.distanceMeters = builder.distanceMeters;
        this.durationSeconds = builder.durationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Waypoint> getTrackPoints() {
        return trackPoints;
    }

    public void setTrackPoints(List<Waypoint> trackPoints) {
        this.trackPoints = trackPoints;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public static class Builder {
        private String status;
        private String message;
        private List<Waypoint> trackPoints;
        private double distanceMeters;
        private long durationSeconds;

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder trackPoints(List<Waypoint> trackPoints) {
            this.trackPoints = trackPoints;
            return this;
        }

        public Builder distanceMeters(double distanceMeters) {
            this.distanceMeters = distanceMeters;
            return this;
        }

        public Builder durationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public RouteResponseDTO build() {
            return new RouteResponseDTO(this);
        }
    }
}