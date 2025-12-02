package ua.askerov.routepal.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.ExportRequestDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ExportService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExportServiceImpl implements ExportService {

    private final WebClient webClient;
    private final String orsProfile;

    public ExportServiceImpl(OrsConfigProperties orsConfig) {
        this.orsProfile = orsConfig.getProfile();

        this.webClient = WebClient.builder()
                .baseUrl(orsConfig.getApi().getUrl())
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .build();
    }

    @Override
    public String exportRoute(ExportRequestDTO request) {
        if (request == null || request.getWaypoints() == null || request.getWaypoints().size() < 2) {
            throw new IllegalArgumentException("Для експорту потрібно мінімум 2 точки");
        }

        String isoTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        String trkName;

        if (request.getRouteName() != null && !request.getRouteName().isBlank()) {
            trkName = request.getRouteName();
        } else {
            String trkDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yy-MM-dd"));
            String trkStat = String.format("%dKM %dH", request.getTotalDistanceKm(), request.getTotalTimeHours());
            trkName = "Route " + trkDate + " " + trkStat;
        }

        String headerTemplate = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx creator="RoutePal" version="1.1"
                  xmlns="http://www.topografix.com/GPX/1/1"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
                  <metadata>
                    <name>RoutePal Export</name>
                    <time>%s</time>
                  </metadata>
                  <trk>
                    <name>%s</name>
                    <trkseg>
                """;

        StringBuilder gpxBuilder = new StringBuilder();
        gpxBuilder.append(headerTemplate.formatted(isoTime, trkName));

        for (Waypoint wp : request.getWaypoints()) {
            gpxBuilder.append(String.format(Locale.US, """
                                  <trkpt lat="%.6f" lon="%.6f">
                                    <ele>%.2f</ele>
                                  </trkpt>
                            """, wp.getLatitude(), wp.getLongitude(), wp.getElevation()));
        }

        String footer = """
                    </trkseg>
                  </trk>
                </gpx>
                """;

        gpxBuilder.append(footer);

        return gpxBuilder.toString();
    }
}