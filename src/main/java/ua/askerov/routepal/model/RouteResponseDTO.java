package ua.askerov.routepal.model;

import java.util.List;

public class RouteResponseDTO {

    private String status;
    private String message;
    private List<Waypoint> trackPoints;
    private double distanceMeters;
    private long durationSeconds;

    // 1. Додаємо поле
    private List<ClimbDTO> climbs;

    public RouteResponseDTO() {
    }

    private RouteResponseDTO(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.trackPoints = builder.trackPoints;
        this.distanceMeters = builder.distanceMeters;
        this.durationSeconds = builder.durationSeconds;
        this.climbs = builder.climbs; // 2. Ініціалізуємо в конструкторі
    }

    // ... Геттери і Сеттери для старих полів ...
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<Waypoint> getTrackPoints() { return trackPoints; }
    public void setTrackPoints(List<Waypoint> trackPoints) { this.trackPoints = trackPoints; }
    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    // 3. Геттер і Сеттер для climbs
    public List<ClimbDTO> getClimbs() {
        return climbs;
    }

    public void setClimbs(List<ClimbDTO> climbs) {
        this.climbs = climbs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String status;
        private String message;
        private List<Waypoint> trackPoints;
        private double distanceMeters;
        private long durationSeconds;
        private List<ClimbDTO> climbs; // 4. Поле в білдері

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

        // 5. Метод білдера
        public Builder climbs(List<ClimbDTO> climbs) {
            this.climbs = climbs;
            return this;
        }

        public RouteResponseDTO build() {
            return new RouteResponseDTO(this);
        }
    }
}