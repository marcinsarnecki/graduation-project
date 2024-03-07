package uwr.ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"uwr.ms"})
public class TravelPlannerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelPlannerApplication.class, args);
    }
}