package ua.askerov.routepal.model;

public class ClimbDTO {
    private int startIndex; // Індекс початку у масиві точок
    private int endIndex;   // Індекс кінця
    private double distanceMeters;
    private double avgGradient; // У відсотках (наприклад, 5.2)
    private double elevationGain; // Набір висоти в метрах

    // Конструктор, геттери, сеттери
    public ClimbDTO(int startIndex, int endIndex, double distanceMeters, double avgGradient, double elevationGain) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.distanceMeters = distanceMeters;
        this.avgGradient = avgGradient;
        this.elevationGain = elevationGain;
    }

    // Getters & Setters...
    public int getStartIndex() { return startIndex; }
    public int getEndIndex() { return endIndex; }
    public double getDistanceMeters() { return distanceMeters; }
    public double getAvgGradient() { return avgGradient; }
    public double getElevationGain() { return elevationGain; }
}