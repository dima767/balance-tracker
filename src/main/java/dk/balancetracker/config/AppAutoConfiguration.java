package dk.balancetracker.config;

import dk.balancetracker.repository.PayeeRepository;
import dk.balancetracker.repository.PaymentItemRepository;
import dk.balancetracker.repository.PaymentPeriodRepository;
import dk.balancetracker.service.DefaultPayeeService;
import dk.balancetracker.service.DefaultPaymentPeriodService;
import dk.balancetracker.service.PayeeService;
import dk.balancetracker.service.PaymentPeriodService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Main application auto-configuration.
 * <p>
 * Defines explicit service beans while relying on Spring Boot auto-configuration for:
 * - JPA repositories (via spring-boot-starter-data-jpa)
 * - Transaction management (via spring-boot-starter-data-jpa)
 * - Entity scanning (from main application package)
 * <p>
 * Only services need explicit @Bean definitions since we avoid @Service stereotypes.
 *
 * @author Dmitriy Kopylenko
 */
@AutoConfiguration
public class AppAutoConfiguration {

    /**
     * Service layer configuration.
     * <p>
     * Defines all service beans with explicit dependency injection.
     */
    @Configuration
    public static class ServiceConfig {

        @Bean
        public PayeeService payeeService(PayeeRepository payeeRepository) {
            return new DefaultPayeeService(payeeRepository);
        }

        @Bean
        public PaymentPeriodService paymentPeriodService(
            PaymentPeriodRepository paymentPeriodRepository,
            PaymentItemRepository paymentItemRepository,
            PayeeService payeeService
        ) {
            return new DefaultPaymentPeriodService(
                paymentPeriodRepository,
                paymentItemRepository,
                payeeService
            );
        }
    }
}
