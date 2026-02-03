package dk.balancetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application.
 * <p>
 * Uses explicit configuration with standard component scanning:
 * - Controllers are discovered via @Controller stereotype in dk.balancetracker.web
 * - All other beans (services, repositories) are defined in AppAutoConfiguration
 * - Auto-configuration is registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *
 * @author Dmitriy Kopylenko
 */
@SpringBootApplication
public class BalanceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BalanceTrackerApplication.class, args);
    }
}
