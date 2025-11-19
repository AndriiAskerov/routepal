package ua.askerov.routepal.model;

// --- DTO: Клас для представлення однієї точки ---
public class Waypoint {
    private double latitude;
    private double longitude;
    private double elevation;

    public Waypoint() {
    }

    public Waypoint(double latitude, double longitude, double elevation) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
    }

    // Сеттери та геттери потрібні для автоматичного мапінгу JSON
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    @Override
    public String toString() {
        return "Waypoint{" + "lat=" + latitude + ", lon=" + longitude + '}';
    }
}