package dk.balancetracker.repository;

import dk.balancetracker.domain.PaymentPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link PaymentPeriod} entity.
 * <p>
 * Provides standard CRUD operations plus custom query methods for
 * finding payment periods by date ranges and other criteria.
 * <p>
 * Auto-configured by Spring Boot's JPA auto-configuration.
 *
 * @author Dmitriy Kopylenko
 */
public interface PaymentPeriodRepository extends JpaRepository<PaymentPeriod, Long> {

    /**
     * Finds a payment period by its exact period date.
     *
     * @param periodDate the period date
     * @return optional containing the payment period if found
     */
    Optional<PaymentPeriod> findByPeriodDate(LocalDate periodDate);

    /**
     * Finds all payment periods within a date range (inclusive).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of payment periods ordered by period date descending
     */
    List<PaymentPeriod> findByPeriodDateBetweenOrderByPeriodDateDesc(
        LocalDate startDate,
        LocalDate endDate
    );

    /**
     * Finds all payment periods ordered by period date descending (most recent first).
     *
     * @return list of all payment periods
     */
    List<PaymentPeriod> findAllByOrderByPeriodDateDesc();

    /**
     * Finds payment periods after a specific date.
     *
     * @param date the cutoff date
     * @return list of payment periods ordered by period date descending
     */
    List<PaymentPeriod> findByPeriodDateAfterOrderByPeriodDateDesc(LocalDate date);

    /**
     * Checks if a payment period exists for a specific date.
     *
     * @param periodDate the period date
     * @return true if a period exists for that date
     */
    boolean existsByPeriodDate(LocalDate periodDate);

    /**
     * Fetches a payment period with its payment items and payees eagerly loaded.
     *
     * @param id the payment period ID
     * @return optional containing the payment period with payment items and payees
     */
    @Query("SELECT pp FROM PaymentPeriod pp LEFT JOIN FETCH pp.paymentItems pi LEFT JOIN FETCH pi.payee WHERE pp.id = :id ORDER BY pi.id ASC")
    Optional<PaymentPeriod> findByIdWithPaymentItems(Long id);

    /**
     * Finds all payment periods with payment items and payees eagerly loaded, ordered by period date descending.
     * Payment items within each period are ordered by ID ascending (chronological order).
     *
     * @return list of all payment periods with payment items and payees
     */
    @Query("SELECT DISTINCT pp FROM PaymentPeriod pp LEFT JOIN FETCH pp.paymentItems pi LEFT JOIN FETCH pi.payee ORDER BY pp.periodDate DESC, pi.id ASC")
    List<PaymentPeriod> findAllWithPaymentItems();
}
