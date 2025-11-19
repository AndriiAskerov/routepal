package ua.askerov.routepal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// Ігноруємо невідомі поля, щоб не створювати DTO для всієї відповіді
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrsResponse {

    @JsonProperty("features")
    private List<OrsFeature> features;

    // Геттери та сеттери
    public List<OrsFeature> getFeatures() { return features; }
    public void setFeatures(List<OrsFeature> features) { this.features = features; }
}