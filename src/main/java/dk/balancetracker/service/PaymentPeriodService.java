package dk.balancetracker.service;

import dk.balancetracker.domain.PaymentPeriod;
import dk.balancetracker.domain.PaymentItem;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing payment periods and their payment items.
 *
 * @author Dmitriy Kopylenko
 */
public interface PaymentPeriodService {

    /**
     * Creates a new payment period.
     *
     * @param periodDate the period date
     * @param startingBalance the starting balance
     * @return the created payment period
     */
    PaymentPeriod createPaymentPeriod(LocalDate periodDate, MonetaryAmount startingBalance);

    /**
     * Creates a new payment period with payment items.
     * All payment items are created atomically with the payment period.
     *
     * @param periodDate the period date
     * @param startingBalance the starting balance
     * @param paymentItems list of payment items to create
     * @return the created payment period with all payment items
     * @throws IllegalArgumentException if validation fails
     */
    PaymentPeriod createPaymentPeriodWithPaymentItems(
        LocalDate periodDate,
        MonetaryAmount startingBalance,
        List<DefaultPaymentPeriodService.PaymentItemData> paymentItems
    );

    /**
     * Finds a payment period by ID with all payment items loaded.
     *
     * @param id the payment period ID
     * @return optional containing the payment period if found
     */
    Optional<PaymentPeriod> findById(Long id);

    /**
     * Finds a payment period by ID with payment items eagerly loaded.
     *
     * @param id the payment period ID
     * @return optional containing the payment period with payment items
     */
    Optional<PaymentPeriod> findByIdWithPaymentItems(Long id);

    /**
     * Finds a payment period by its period date.
     *
     * @param periodDate the period date
     * @return optional containing the payment period if found
     */
    Optional<PaymentPeriod> findByPeriodDate(LocalDate periodDate);

    /**
     * Finds all payment periods ordered by period date (most recent first).
     *
     * @return list of all payment periods
     */
    List<PaymentPeriod> findAll();

    /**
     * Finds all payment periods with payment items eagerly loaded, ordered by period date (most recent first).
     *
     * @return list of all payment periods with payment items
     */
    List<PaymentPeriod> findAllWithPaymentItems();

    /**
     * Finds payment periods within a date range.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of payment periods in the range
     */
    List<PaymentPeriod> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Updates an existing payment period.
     *
     * @param id the payment period ID
     * @param periodDate the new period date
     * @param startingBalance the new starting balance
     * @return the updated payment period
     * @throws IllegalArgumentException if payment period not found
     */
    PaymentPeriod updatePaymentPeriod(Long id, LocalDate periodDate, MonetaryAmount startingBalance);

    /**
     * Updates a payment period and replaces all payment items atomically.
     * This ensures the entire operation succeeds or fails as a unit.
     * All existing payment items are removed and replaced with the new ones.
     *
     * @param id the payment period ID
     * @param periodDate the new period date
     * @param startingBalance the new starting balance
     * @param paymentItems the new payment items (replaces all existing)
     * @return the updated payment period with all items
     * @throws IllegalArgumentException if payment period not found or validation fails
     */
    PaymentPeriod updatePaymentPeriodWithPaymentItems(
        Long id,
        LocalDate periodDate,
        MonetaryAmount startingBalance,
        List<DefaultPaymentPeriodService.PaymentItemData> paymentItems
    );

    /**
     * Deletes a payment period and all its payment items.
     *
     * @param id the payment period ID
     */
    void deletePaymentPeriod(Long id);

    /**
     * Adds a payment item to a payment period.
     *
     * @param paymentPeriodId the payment period ID
     * @param amount the payment amount
     * @param payeeId the payee ID (if existing payee selected)
     * @param payeeName the payee name (if new payee to be created)
     * @param notes optional notes
     * @return the created payment item
     * @throws IllegalArgumentException if payment period not found
     */
    PaymentItem addPaymentItem(Long paymentPeriodId, MonetaryAmount amount,
                                Long payeeId, String payeeName, String notes);

    /**
     * Updates a payment item.
     *
     * @param paymentPeriodId the payment period ID
     * @param paymentItemId the payment item ID
     * @param amount the new amount
     * @param payeeId the payee ID (if existing payee selected)
     * @param payeeName the payee name (if new payee to be created)
     * @param notes the new notes
     * @return the updated payment item
     * @throws IllegalArgumentException if payment period or payment item not found
     */
    PaymentItem updatePaymentItem(Long paymentPeriodId, Long paymentItemId, MonetaryAmount amount,
                                   Long payeeId, String payeeName, String notes);

    /**
     * Removes a payment item from a payment period.
     *
     * @param paymentPeriodId the payment period ID
     * @param paymentItemId the payment item ID
     * @throws IllegalArgumentException if payment period or payment item not found
     */
    void removePaymentItem(Long paymentPeriodId, Long paymentItemId);

    /**
     * Checks if a payment period exists for a specific date.
     *
     * @param periodDate the period date
     * @return true if a period exists for that date
     */
    boolean existsByPeriodDate(LocalDate periodDate);
}
