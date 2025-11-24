package ua.askerov.routepal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ua.askerov.routepal.config.OrsConfigProperties;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ApiUsageAuditor {

    // --- ЛІМІТИ ---
    private final long DIRECTIONS_LIMIT;
    private final long EXPORT_LIMIT;
    private final long ELEVATION_LINE_LIMIT;

    // --- ЛІЧИЛЬНИКИ (Атомарні для безпеки потоків) ---
    private final AtomicLong directionsCount = new AtomicLong(0);
    private final AtomicLong exportCount = new AtomicLong(0);
    private final AtomicLong elevationLineCount = new AtomicLong(0);
    // -- JSON ---
    private final ObjectMapper objectMapper;
    private final File statsFile = new File("src/main/resources/ors-api-stats.json");
    private LocalDate lastResetDate = LocalDate.now();

    public ApiUsageAuditor(OrsConfigProperties orsConfig) {
        this.DIRECTIONS_LIMIT = orsConfig.getApi().getLimit().getDirections().getDaily();
        this.EXPORT_LIMIT = orsConfig.getApi().getLimit().getExport().getDaily();
        this.ELEVATION_LINE_LIMIT = orsConfig.getApi().getLimit().getElevation().getLine().getDaily();
        this.objectMapper = new ObjectMapper();
    }


    public void updateLimitCounters() {
        if (!LocalDate.now().isEqual(lastResetDate)) {
            System.out.println("Новий день! Ліміт запитів оновлено");
            lastResetDate = LocalDate.now();
            directionsCount.set(0);
            exportCount.set(0);
            elevationLineCount.set(0);
            persistStats();
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    // Кожної нової доби (опівночі, 0:00 за локальним часом сервера) скидає лічильники
    public void scheduledDailyReset() {
        updateLimitCounters();
    }

    // Дозвіл на запит
    public boolean tryIncrementCounter(AtomicLong counter, long limit) {
        // 1. Актуалізація лічильників (перевірка оновлення дати)
        updateLimitCounters();

        // 2. Оптимістичний цикл (Concurrency)
        while (true) {
            long currentValue = counter.get();
            // 3. Перевірка ліміту
            if (currentValue >= limit) {
                // Ліміт вичерпано
                return false;
            }
            // 4. Спроба атомарного інкременту
            long nextValue = currentValue + 1;
            // Якщо лічильник ДОСІ дорівнює currentValue - встанови його на currentValue + 1
            if (counter.compareAndSet(currentValue, nextValue)) {
                persistStats(); // Зберігаємо інкремент
                return true;
            }
            // 5. Якийсь інший потік вже змінив значення - повертаємось на початок циклу
        }
    }

    public boolean tryIncrementDirectionsCounter() {
        return tryIncrementCounter(directionsCount, DIRECTIONS_LIMIT);
    }

    public boolean tryIncrementExportCounter() {
        return tryIncrementCounter(exportCount, EXPORT_LIMIT);
    }

    public boolean tryIncrementElevationCounter() {
        return tryIncrementCounter(elevationLineCount, ELEVATION_LINE_LIMIT);
    }

    @PostConstruct // Викликається під час запуску сервера
    public void loadStats() {
        if (statsFile.exists()) {
            try {
                Map<String, Object> stats = objectMapper.readValue(statsFile, Map.class);
                LocalDate savedDate = LocalDate.parse((String) stats.get("date"));

                // Якщо збережений файл за сьогодні - відновлюємо лічильник
                if (savedDate.isEqual(LocalDate.now())) {
                    this.lastResetDate = savedDate;
                    this.directionsCount.set(((Number) stats.get("directionsCount")).longValue());
                    this.exportCount.set(((Number) stats.get("exportCount")).longValue());
                    this.elevationLineCount.set(((Number) stats.get("elevationLineCount")).longValue());
                    System.out.printf("""
                            Статистика API відновлена:
                            \t- Directions: %d/%d
                            \t- Export: %d/%d
                            \t- Elevation Line: %d/%d
                            """, directionsCount.get(), DIRECTIONS_LIMIT, exportCount.get(), EXPORT_LIMIT, elevationLineCount.get(), ELEVATION_LINE_LIMIT);
                } else {
                    // Якщо файл за вчора, просто починаємо з нуля
                    System.out.println("Статистика API застаріла, починаємо з нуля");
                }
            } catch (IOException e) {
                System.err.println("Не вдалося завантажити статистику API: " + e.getMessage());
            }
        }
        updateLimitCounters();
    }

    @PreDestroy // Викликається при зупинці сервера
    // Зберігає стан лічильників (JSON)
    public void persistStats() {
        try {
            // Зберігаємо нові лічильники
            Map<String, Object> stats = Map.of("date", lastResetDate.toString(), "directionsCount", directionsCount.get(), "exportCount", exportCount.get(), "elevationLineCount", elevationLineCount.get());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(statsFile, stats);
        } catch (IOException e) {
            System.err.println("Не вдалося зберегти статистику API: " + e.getMessage());
        }
    }
}