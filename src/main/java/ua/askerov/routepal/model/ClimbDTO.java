package ua.askerov.routepal.model;

public class ClimbDTO {
    private int startIndex;       // Індекс початку у масиві точок
    private int endIndex;         // Індекс кінця
    private double distanceMeters;
    private double avgGradient;   // У відсотках (наприклад, 5.2)
    private double elevationGain; // Набір висоти в метрах
    private String category;      // Нове поле: "Cat 4", "Тяжкий", "Середній"

    // 1. Пустий конструктор (ОБОВ'ЯЗКОВО для new ClimbDTO() у сервісі та для JSON/Jackson)
    public ClimbDTO() {
    }

    // 2. Конструктор з усіма полями (для зручності, якщо знадобиться)
    public ClimbDTO(int startIndex, int endIndex, double distanceMeters, double avgGradient, double elevationGain, String category) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.distanceMeters = distanceMeters;
        this.avgGradient = avgGradient;
        this.elevationGain = elevationGain;
        this.category = category;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getAvgGradient() {
        return avgGradient;
    }

    public void setAvgGradient(double avgGradient) {
        this.avgGradient = avgGradient;
    }

    public double getElevationGain() {
        return elevationGain;
    }

    public void setElevationGain(double elevationGain) {
        this.elevationGain = elevationGain;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}