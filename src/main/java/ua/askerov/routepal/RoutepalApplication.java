package ua.askerov.routepal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RoutepalApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutepalApplication.class, args);
    }

}
