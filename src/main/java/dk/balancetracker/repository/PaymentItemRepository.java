package dk.balancetracker.repository;

import dk.balancetracker.domain.PaymentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for {@link PaymentItem} entity.
 * <p>
 * Provides standard CRUD operations plus custom query methods for
 * finding payment items by payment period.
 * <p>
 * Auto-configured by Spring Boot's JPA auto-configuration.
 *
 * @author Dmitriy Kopylenko
 */
public interface PaymentItemRepository extends JpaRepository<PaymentItem, Long> {

    /**
     * Finds all payment items for a specific payment period.
     *
     * @param paymentPeriodId the payment period ID
     * @return list of payment items
     */
    List<PaymentItem> findByPaymentPeriodId(Long paymentPeriodId);

    /**
     * Finds payment items by notes containing search term (case-insensitive).
     *
     * @param searchTerm the search term
     * @return list of matching payment items
     */
    List<PaymentItem> findByNotesContainingIgnoreCase(String searchTerm);

    /**
     * Deletes all payment items for a specific payment period.
     *
     * @param paymentPeriodId the payment period ID
     */
    void deleteByPaymentPeriodId(Long paymentPeriodId);

    /**
     * Counts payment items for a specific payment period.
     *
     * @param paymentPeriodId the payment period ID
     * @return count of payment items
     */
    long countByPaymentPeriodId(Long paymentPeriodId);
}
