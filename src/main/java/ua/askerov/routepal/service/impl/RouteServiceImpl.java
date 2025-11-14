package ua.askerov.routepal.service.impl;

import org.springframework.stereotype.Service;
import ua.askerov.routepal.model.DetailedRoute;
import ua.askerov.routepal.model.Waypoint;
import ua.askerov.routepal.service.RouteService;

import java.util.List;

// @Service позначає клас як компонент бізнес-логіки Spring
@Service
public class RouteServiceImpl implements RouteService {

    @Override
    public DetailedRoute calculateAndFetchRoute(List<Waypoint> waypoints) {
        // *** 1. Тут буде логіка виклику зовнішнього API маршрутизації ***
        //    - Формування URL запиту
        //    - Виконання HTTP-запиту (за допомогою RestTemplate або WebClient)
        //    - Парсинг JSON-відповіді у внутрішній об'єкт DetailedRoute

        // *** 2. Тут буде логіка отримання висот *** (Якщо не входить у API маршрутизації)

        System.out.println("Сервіс обробляє точки...");
        // ... (тимчасова заглушка)

        return new DetailedRoute(); // Повернення заглушки
    }
}