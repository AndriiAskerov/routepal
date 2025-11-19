package ua.askerov.routepal.model;

public class RouteResponseDTO {

    private String status;
    private String message;
    private String polyline;            // Закодований маршрут для малювання на карті
    private double distanceMeters;      // Загальна відстань
    private long durationSeconds;       // Загальний час у дорозі

    public RouteResponseDTO(String status, String message) {
        this.status = status;
        this.message = message;
        this.polyline = "";
        this.distanceMeters = 0L;
        this.durationSeconds = 0;
    }

    public RouteResponseDTO(String status, String message, String polyline, double distanceMeters, long durationSeconds) {
        this.status = status;
        this.message = message;
        this.polyline = polyline;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
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

    public String getPolyline() {
        return polyline;
    }

    public void setPolyline(String polyline) {
        this.polyline = polyline;
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
}