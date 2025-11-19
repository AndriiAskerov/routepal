package ua.askerov.routepal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrsFeature {

    @JsonProperty("geometry")
    private OrsGeometry geometry;

    // Геттери та сеттери
    public OrsGeometry getGeometry() { return geometry; }
    public void setGeometry(OrsGeometry geometry) { this.geometry = geometry; }
}