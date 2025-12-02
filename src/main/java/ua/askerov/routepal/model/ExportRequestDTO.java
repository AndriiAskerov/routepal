package ua.askerov.routepal.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportRequestDTO {
    private List<Waypoint> waypoints;
    private String routeName;
    private int totalDistanceKm;
    private int totalTimeHours;

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public int getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(int totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public int getTotalTimeHours() {
        return totalTimeHours;
    }

    public void setTotalTimeHours(int totalTimeHours) {
        this.totalTimeHours = totalTimeHours;
    }

    /**
     * Генерує: сustomName.gpx, або route_yy-MM-dd_50KM_2H.gpx
     * Дозволяємо лише букви, цифри, тире та підкреслення. Решту міняємо на "_"
     * Підтримка кирилиці включена в діапазон
     * Інакше генеруємо по шаблону: route_25-12-01_100KM_4H.gpx
     */
    public String generateFilename() {
        if (routeName != null && !routeName.isBlank()) {
            String filename = routeName.trim().replaceAll("[^a-zA-Z0-9\\-а-яА-ЯіIїЇєЄ_]", "_");
            return filename + ".gpx";
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yy-MM-dd"));
        return String.format("route_%s_%dKM_%dH.gpx",
                dateStr,
                totalDistanceKm,
                totalTimeHours);
    }
}