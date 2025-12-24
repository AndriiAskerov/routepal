package ua.askerov.routepal.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.askerov.routepal.config.OrsConfigProperties;
import ua.askerov.routepal.model.ClimbDTO;
import ua.askerov.routepal.model.ElevationResponseDTO;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.ElevationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElevationServiceImpl implements ElevationService {

    private final WebClient webClient;

    // === КОНСТАНТИ ===
    private static final int SMOOTHING_WINDOW = 5;

    // Етап 1: Визначення "мікро-підйомів"
    private static final double MIN_MICRO_SEGMENT_GAIN = 5.0;

    // Етап 2: Об'єднання (Кластеризація)
    private static final double MAX_MERGE_DROP = 25.0;
    private static final double MAX_MERGE_DISTANCE = 600.0;

    // Етап 3: Обрізка країв (Refining)
    private static final double MIN_EDGE_GRADIENT = 2.0; // % - ігноруємо пологі входи/виходи
    private static final double MIN_FINAL_CLIMB_DISTANCE = 300.0; // м - мінімальна довжина фінальної гори

    public ElevationServiceImpl(OrsConfigProperties orsConfig) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openrouteservice.org")
                .defaultHeader("Authorization", orsConfig.getApi().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public ElevationResponseDTO getElevationForTrack(List<Waypoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) {
            return new ElevationResponseDTO(new ArrayList<>(), new ArrayList<>(), 0.0);
        }

        // 1. Отримання даних (вже працює коректно)
        List<Waypoint> enrichedPoints = fetchElevationData(trackPoints);
        if (enrichedPoints.isEmpty()) {
            enrichedPoints = new ArrayList<>(trackPoints);
        }

        // 2. Згладжування
        List<Waypoint> smoothedPoints = smoothElevation(enrichedPoints);

        // 3. Пошук підйомів
        List<TempSegment> microSegments = findMicroSegments(smoothedPoints);
        List<ClimbDTO> roughClimbs = clusterSegments(microSegments, smoothedPoints);
        List<ClimbDTO> finalClimbs = refineClimbs(roughClimbs, smoothedPoints);

        // 4. === ОБЧИСЛЕННЯ ЗАГАЛЬНОГО НАБОРУ ===
        // Переконайтеся, що цей рядок є у вашому коді!
        double totalAscent = calculateTotalAscent(smoothedPoints);

        // ЛОГ: Перевіряємо, що нарахували
        System.out.println(">>> FINAL TOTAL ASCENT: " + totalAscent);

        // 5. Повернення DTO
        // Важливо: переконайтеся, що третій параметр тут саме totalAscent
        return new ElevationResponseDTO(smoothedPoints, finalClimbs, totalAscent);
    }

    // === НОВИЙ ДОПОМІЖНИЙ МЕТОД ===
    private double calculateTotalAscent(List<Waypoint> points) {
        double ascent = 0;
        if (points.size() < 2) return 0;

        for (int i = 0; i < points.size() - 1; i++) {
            double diff = points.get(i + 1).getElevation() - points.get(i).getElevation();
            // Сумуємо тільки позитивні зміни висоти
            if (diff > 0) {
                ascent += diff;
            }
        }
        return ascent;
    }

    // Внутрішній клас для проміжних розрахунків
    private static class TempSegment {
        int startIdx;
        int endIdx;
        double startEle;
        double endEle;

        public TempSegment(int startIdx, int endIdx, double startEle, double endEle) {
            this.startIdx = startIdx;
            this.endIdx = endIdx;
            this.startEle = startEle;
            this.endEle = endEle;
        }
    }

    // ... (Методи findMicroSegments та clusterSegments залишаються без змін) ...

    private List<TempSegment> findMicroSegments(List<Waypoint> points) {
        List<TempSegment> segments = new ArrayList<>();
        if (points.size() < 2) return segments;

        int startIdx = -1;
        double noiseTolerance = 0.5;

        for (int i = 0; i < points.size() - 1; i++) {
            double currEle = points.get(i).getElevation();
            double nextEle = points.get(i+1).getElevation();
            double diff = nextEle - currEle;

            if (diff > 0) {
                if (startIdx == -1) startIdx = i;
            } else if (diff < -noiseTolerance) {
                if (startIdx != -1) {
                    closeMicroSegment(segments, points, startIdx, i);
                    startIdx = -1;
                }
            }
        }
        if (startIdx != -1) {
            closeMicroSegment(segments, points, startIdx, points.size() - 1);
        }
        return segments;
    }

    private void closeMicroSegment(List<TempSegment> segments, List<Waypoint> points, int start, int end) {
        double sEle = points.get(start).getElevation();
        double eEle = points.get(end).getElevation();
        if ((eEle - sEle) >= MIN_MICRO_SEGMENT_GAIN) {
            segments.add(new TempSegment(start, end, sEle, eEle));
        }
    }

    private List<ClimbDTO> clusterSegments(List<TempSegment> microSegments, List<Waypoint> points) {
        List<ClimbDTO> roughClimbs = new ArrayList<>();
        if (microSegments.isEmpty()) return roughClimbs;

        TempSegment currentCluster = microSegments.get(0);

        for (int i = 1; i < microSegments.size(); i++) {
            TempSegment nextSegment = microSegments.get(i);
            double gapDistance = calculateDistance(points, currentCluster.endIdx, nextSegment.startIdx);
            double dropInGap = currentCluster.endEle - nextSegment.startEle;

            boolean isGapSmall = gapDistance <= MAX_MERGE_DISTANCE;
            boolean isDropAcceptable = dropInGap <= MAX_MERGE_DROP;

            if (isGapSmall && isDropAcceptable) {
                currentCluster.endIdx = nextSegment.endIdx;
                currentCluster.endEle = nextSegment.endEle;
            } else {
                // Додаємо в roughClimbs, але поки без категорій (категорії дамо після обрізки)
                addRoughClimb(roughClimbs, points, currentCluster);
                currentCluster = nextSegment;
            }
        }
        addRoughClimb(roughClimbs, points, currentCluster);
        return roughClimbs;
    }

    private void addRoughClimb(List<ClimbDTO> results, List<Waypoint> points, TempSegment segment) {
        // Просто створюємо DTO з індексами, детальні обрахунки будуть в refineClimbs
        ClimbDTO dto = new ClimbDTO();
        dto.setStartIndex(segment.startIdx);
        dto.setEndIndex(segment.endIdx);
        // Попередні дані, щоб не губити (хоча вони перерахуються)
        dto.setElevationGain(segment.endEle - segment.startEle);
        results.add(dto);
    }

    /**
     * ПРОХІД 3: Верифікація та обрізка "брудних" країв.
     * Тут ми перетворюємо "сирі" кластери на чистові підйоми.
     */
    private List<ClimbDTO> refineClimbs(List<ClimbDTO> rawClimbs, List<Waypoint> allPoints) {
        List<ClimbDTO> refinedClimbs = new ArrayList<>();

        for (ClimbDTO climb : rawClimbs) {
            // 1. Працюємо з підмножиною точок для зручності пошуку відносних індексів
            // (Важливо: subList створює view, тому не копіює дані масиву)
            List<Waypoint> climbPoints = allPoints.subList(climb.getStartIndex(), climb.getEndIndex() + 1);

            // 2. Обрізаємо "млявий" початок
            int relativeStartOffset = findHardStart(climbPoints);

            // 3. Обрізаємо "млявий" кінець (передаємо offset, щоб не зайти за початок)
            int relativeEndOffset = findHardEnd(climbPoints, relativeStartOffset);

            // Перераховуємо глобальні індекси
            int absoluteStartIndex = climb.getStartIndex() + relativeStartOffset;
            int absoluteEndIndex = climb.getStartIndex() + relativeEndOffset;

            // 4. Валідація і фінальний розрахунок
            double newDistance = calculateDistance(allPoints, absoluteStartIndex, absoluteEndIndex);
            double newGain = allPoints.get(absoluteEndIndex).getElevation() - allPoints.get(absoluteStartIndex).getElevation();

            // Захист від ділення на нуль
            double newGradient = (newDistance > 0) ? (newGain / newDistance) * 100 : 0;

            // Перевіряємо, чи підйом все ще валідний після "хірургії"
            // (Використовуємо трохи м'якший фільтр для градієнта тут, бо це середній по всьому шматку)
            if (newDistance >= MIN_FINAL_CLIMB_DISTANCE && newGain > 10) {
                // Логіка isClimbValid перенесена сюди або викликається окремо
                if (isClimbValid(newDistance, newGain, newGradient)) {
                    ClimbDTO refined = new ClimbDTO();
                    refined.setStartIndex(absoluteStartIndex);
                    refined.setEndIndex(absoluteEndIndex);
                    refined.setDistanceMeters(newDistance);
                    refined.setElevationGain(newGain);
                    refined.setAvgGradient(newGradient);

                    assignCategory(refined);

                    System.out.printf("REFINED CLIMB: Dist %.0fm, Gain %.0fm, Grad %.1f%%\n", newDistance, newGain, newGradient);
                    refinedClimbs.add(refined);
                }
            }
        }
        return refinedClimbs;
    }

    private int findHardStart(List<Waypoint> points) {
        // Ковзне вікно: перевіряємо, чи наступні 5 точок дають нормальний градієнт
        int window = 5;
        for (int i = 0; i < points.size() - window; i++) {
            double localGrad = calculateLocalGradient(points, i, i + window);
            if (localGrad >= MIN_EDGE_GRADIENT) {
                return i; // Знайшли початок "стінки"
            }
        }
        return 0; // Якщо не знайшли крутого початку, лишаємо як є
    }

    private int findHardEnd(List<Waypoint> points, int startOffset) {
        int window = 5;
        // Йдемо з кінця назад
        for (int i = points.size() - 1; i > startOffset + window; i--) {
            double localGrad = calculateLocalGradient(points, i - window, i);
            if (localGrad >= MIN_EDGE_GRADIENT) {
                return i; // Знайшли кінець "стінки"
            }
        }
        return points.size() - 1;
    }

    private double calculateLocalGradient(List<Waypoint> points, int idx1, int idx2) {
        if (idx2 >= points.size()) idx2 = points.size() - 1;

        // Використовуємо існуючий метод distance
        double dist = 0;
        // Рахуємо дистанцію покроково між точками, бо пряма лінія (distance(p1, p2)) може бути неточною на дугах
        for(int k=idx1; k<idx2; k++) {
            dist += distance(points.get(k), points.get(k+1));
        }

        double gain = points.get(idx2).getElevation() - points.get(idx1).getElevation();

        if (dist <= 0) return 0;
        return (gain / dist) * 100.0;
    }

    private boolean isClimbValid(double dist, double gain, double grad) {
        if (gain < 15) return false;
        if (gain >= 20 && grad >= 4.0) return true;
        if (dist >= 300 && grad >= 2.0) return true;
        if (gain >= 35 && grad >= 1.0) return true;
        return false;
    }

    private void assignCategory(ClimbDTO climb) {
        double score = climb.getDistanceMeters() * climb.getAvgGradient();
        if (score >= 80000) climb.setCategory("HC");
        else if (score >= 32000) climb.setCategory("Cat 1");
        else if (score >= 16000) climb.setCategory("Cat 2");
        else if (score >= 8000) climb.setCategory("Cat 3");
        else if (score >= 3500) climb.setCategory("Cat 4");
        else climb.setCategory("Hill");
    }

    // === UTILS (існуючі) ===

    private List<Waypoint> smoothElevation(List<Waypoint> input) {
        if (input.size() < SMOOTHING_WINDOW) return new ArrayList<>(input);
        List<Waypoint> smoothed = new ArrayList<>();
        int half = SMOOTHING_WINDOW / 2;
        for (int i = 0; i < input.size(); i++) {
            double sum = 0;
            int count = 0;
            for (int j = i - half; j <= i + half; j++) {
                if (j >= 0 && j < input.size()) {
                    sum += input.get(j).getElevation();
                    count++;
                }
            }
            Waypoint p = input.get(i);
            smoothed.add(new Waypoint(p.getLatitude(), p.getLongitude(), sum / count));
        }
        return smoothed;
    }

    private double calculateDistance(List<Waypoint> points, int start, int end) {
        double dist = 0;
        for (int i = start; i < end; i++) {
            dist += distance(points.get(i), points.get(i+1));
        }
        return dist;
    }

    private double distance(Waypoint p1, Waypoint p2) {
        double R = 6371e3;
        double lat1 = Math.toRadians(p1.getLatitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double dLat = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double dLon = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private List<Waypoint> fetchElevationData(List<Waypoint> trackPoints) {
        try {
            // Формуємо список координат [lon, lat]
            List<double[]> coordinates = trackPoints.stream()
                    .map(wp -> new double[]{wp.getLongitude(), wp.getLatitude()})
                    .toList();

            // Явно вказуємо формат відповіді (geojson)
            Map<String, Object> requestBody = Map.of(
                    "format_in", "polyline",
                    "format_out", "geojson", // <--- ДОДАНО: просимо конкретний формат
                    "geometry", coordinates
            );

            System.out.println(">>> Sending elevation request for " + coordinates.size() + " points");

            JsonNode response = webClient.post()
                    .uri("/elevation/line")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            List<Waypoint> enriched = new ArrayList<>();
            if (response != null) {
                // Логіка "подвійного пошуку" координат
                JsonNode coords = response.path("geometry").path("coordinates");

                // Якщо не знайшли в geometry, шукаємо в корені (інколи буває так)
                if (coords.isMissingNode() || !coords.isArray()) {
                    coords = response.path("coordinates");
                }

                if (coords.isArray()) {
                    System.out.println(">>> Received " + coords.size() + " elevation points");
                    for (JsonNode pt : coords) {
                        // ORS повертає масив: [longitude, latitude, elevation]
                        double lon = pt.get(0).asDouble();
                        double lat = pt.get(1).asDouble();
                        double ele = pt.get(2).asDouble();
                        enriched.add(new Waypoint(lat, lon, ele));
                    }
                } else {
                    System.err.println(">>> ERROR: JSON structure mismatch! Response: " + response);
                }
            }
            return enriched;
        } catch (Exception e) {
            System.err.println(">>> API Error: " + e.getMessage());
            // Повертаємо пустий список, щоб далі спрацював fallback (і повернув 0, але хоч без крашу)
            return new ArrayList<>();
        }
    }
}