package ua.askerov.routepal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrsGeometry {

    // Це найважливіше поле!
    // ORS повертає масив масивів: [ [lon, lat, elevation], [lon, lat, elevation], ... ]
    @JsonProperty("coordinates")
    private List<List<Double>> coordinates;

    // Геттери та сеттери
    public List<List<Double>> getCoordinates() { return coordinates; }
    public void setCoordinates(List<List<Double>> coordinates) { this.coordinates = coordinates; }
}