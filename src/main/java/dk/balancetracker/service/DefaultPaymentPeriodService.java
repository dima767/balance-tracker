package dk.balancetracker.service;

import dk.balancetracker.domain.Payee;
import dk.balancetracker.domain.PaymentPeriod;
import dk.balancetracker.domain.PaymentItem;
import dk.balancetracker.repository.PaymentPeriodRepository;
import dk.balancetracker.repository.PaymentItemRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PaymentPeriodService}.
 * <p>
 * Manages payment periods and their associated payment items,
 * including balance calculations and relationship management.
 * <p>
 * Configured as a bean in {@link dk.balancetracker.config.AppAutoConfiguration.ServiceConfig}.
 *
 * @author Dmitriy Kopylenko
 */
@Transactional
public class DefaultPaymentPeriodService implements PaymentPeriodService {

    /**
     * Simple data carrier for payment item information.
     * Used to pass payment item data during payment period creation.
     */
    public record PaymentItemData(
        MonetaryAmount amount,
        Long payeeId,
        String payeeName,
        String notes
    ) {
        public PaymentItemData {
            if (amount == null) {
                throw new IllegalArgumentException("Amount cannot be null");
            }
            if (payeeId == null && (payeeName == null || payeeName.isBlank())) {
                throw new IllegalArgumentException("Either payeeId or payeeName must be provided");
            }
        }
    }

    private final PaymentPeriodRepository paymentPeriodRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final PayeeService payeeService;

    public DefaultPaymentPeriodService(
        PaymentPeriodRepository paymentPeriodRepository,
        PaymentItemRepository paymentItemRepository,
        PayeeService payeeService
    ) {
        this.paymentPeriodRepository = paymentPeriodRepository;
        this.paymentItemRepository = paymentItemRepository;
        this.payeeService = payeeService;
    }

    @Override
    public PaymentPeriod createPaymentPeriod(LocalDate periodDate, MonetaryAmount startingBalance) {
        if (periodDate == null) {
            throw new IllegalArgumentException("Period date cannot be null");
        }
        if (startingBalance == null) {
            throw new IllegalArgumentException("Starting balance cannot be null");
        }

        if (existsByPeriodDate(periodDate)) {
            throw new IllegalArgumentException(
                "Payment period already exists for date: " + periodDate
            );
        }

        PaymentPeriod paymentPeriod = new PaymentPeriod(periodDate, startingBalance);
        return paymentPeriodRepository.save(paymentPeriod);
    }

    @Override
    public PaymentPeriod createPaymentPeriodWithPaymentItems(
        LocalDate periodDate,
        MonetaryAmount startingBalance,
        List<PaymentItemData> paymentItems
    ) {
        if (periodDate == null) {
            throw new IllegalArgumentException("Period date cannot be null");
        }
        if (startingBalance == null) {
            throw new IllegalArgumentException("Starting balance cannot be null");
        }
        if (paymentItems == null) {
            throw new IllegalArgumentException("Payment items list cannot be null");
        }

        if (existsByPeriodDate(periodDate)) {
            throw new IllegalArgumentException(
                "Payment period already exists for date: " + periodDate
            );
        }

        // Create payment period
        PaymentPeriod paymentPeriod = new PaymentPeriod(periodDate, startingBalance);

        // Add all payment items
        int displayOrder = 0;
        for (PaymentItemData data : paymentItems) {
            // Validate currency matches
            if (!data.amount().getCurrency().equals(startingBalance.getCurrency())) {
                throw new IllegalArgumentException(
                    "Payment item currency must match payment period currency: " +
                    startingBalance.getCurrency() +
                    " (found: " + data.amount().getCurrency() + ")"
                );
            }

            // Resolve payee (find existing or create new)
            Payee payee;
            if (data.payeeId() != null) {
                payee = payeeService.findById(data.payeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + data.payeeId()));
            } else {
                payee = payeeService.findOrCreatePayee(data.payeeName());
            }

            PaymentItem paymentItem = new PaymentItem(data.amount(), payee, data.notes());
            paymentItem.setDisplayOrder(displayOrder++);
            paymentPeriod.addPaymentItem(paymentItem);
        }

        // Explicitly recalculate balance before saving to ensure correctness
        paymentPeriod.calculateEndingBalance();

        // Save (cascade will save payment items automatically)
        return paymentPeriodRepository.save(paymentPeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentPeriod> findById(Long id) {
        return paymentPeriodRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentPeriod> findByIdWithPaymentItems(Long id) {
        return paymentPeriodRepository.findByIdWithPaymentItems(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentPeriod> findByPeriodDate(LocalDate periodDate) {
        return paymentPeriodRepository.findByPeriodDate(periodDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPeriod> findAll() {
        return paymentPeriodRepository.findAllByOrderByPeriodDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPeriod> findAllWithPaymentItems() {
        return paymentPeriodRepository.findAllWithPaymentItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPeriod> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        return paymentPeriodRepository.findByPeriodDateBetweenOrderByPeriodDateDesc(
            startDate, endDate
        );
    }

    @Override
    public PaymentPeriod updatePaymentPeriod(
        Long id,
        LocalDate periodDate,
        MonetaryAmount startingBalance
    ) {
        PaymentPeriod paymentPeriod = paymentPeriodRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        if (periodDate != null && !periodDate.equals(paymentPeriod.getPeriodDate())) {
            // Check if another period exists for the new date
            if (existsByPeriodDate(periodDate)) {
                throw new IllegalArgumentException(
                    "Payment period already exists for date: " + periodDate
                );
            }
            paymentPeriod.setPeriodDate(periodDate);
        }

        if (startingBalance != null) {
            paymentPeriod.setStartingBalance(startingBalance);
        }

        return paymentPeriodRepository.save(paymentPeriod);
    }

    @Override
    public void deletePaymentPeriod(Long id) {
        if (!paymentPeriodRepository.existsById(id)) {
            throw new IllegalArgumentException("Payment period not found: " + id);
        }
        paymentPeriodRepository.deleteById(id);
    }

    @Override
    public PaymentItem addPaymentItem(
        Long paymentPeriodId,
        MonetaryAmount amount,
        Long payeeId,
        String payeeName,
        String notes
    ) {
        PaymentPeriod paymentPeriod = paymentPeriodRepository
            .findByIdWithPaymentItems(paymentPeriodId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment period not found: " + paymentPeriodId
            ));

        if (amount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null");
        }
        if (payeeId == null && (payeeName == null || payeeName.isBlank())) {
            throw new IllegalArgumentException("Either payeeId or payeeName must be provided");
        }

        // Validate currency matches
        if (!amount.getCurrency().equals(paymentPeriod.getStartingBalance().getCurrency())) {
            throw new IllegalArgumentException(
                "Payment currency must match payment period currency: " +
                paymentPeriod.getStartingBalance().getCurrency()
            );
        }

        // Resolve payee (find existing or create new)
        Payee payee;
        if (payeeId != null) {
            payee = payeeService.findById(payeeId)
                .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + payeeId));
        } else {
            payee = payeeService.findOrCreatePayee(payeeName);
        }

        // Determine next display order (max + 1, or 0 if no items)
        int nextDisplayOrder = paymentPeriod.getPaymentItems().stream()
            .map(PaymentItem::getDisplayOrder)
            .filter(order -> order != null)
            .max(Integer::compareTo)
            .map(max -> max + 1)
            .orElse(0);

        PaymentItem paymentItem = new PaymentItem(amount, payee, notes);
        paymentItem.setDisplayOrder(nextDisplayOrder);
        paymentPeriod.addPaymentItem(paymentItem);

        // Explicitly recalculate balance before saving to ensure correctness
        paymentPeriod.calculateEndingBalance();

        paymentPeriodRepository.save(paymentPeriod);

        return paymentItem;
    }

    @Override
    public PaymentItem updatePaymentItem(
        Long paymentPeriodId,
        Long paymentItemId,
        MonetaryAmount amount,
        Long payeeId,
        String payeeName,
        String notes
    ) {
        PaymentPeriod paymentPeriod = paymentPeriodRepository
            .findByIdWithPaymentItems(paymentPeriodId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment period not found: " + paymentPeriodId
            ));

        PaymentItem paymentItem = paymentItemRepository.findById(paymentItemId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment item not found: " + paymentItemId
            ));

        // Verify the payment item belongs to this payment period
        if (!paymentItem.getPaymentPeriod().getId().equals(paymentPeriodId)) {
            throw new IllegalArgumentException(
                "Payment item does not belong to payment period: " + paymentPeriodId
            );
        }

        if (amount != null) {
            // Validate currency matches
            if (!amount.getCurrency().equals(paymentPeriod.getStartingBalance().getCurrency())) {
                throw new IllegalArgumentException(
                    "Payment currency must match payment period currency: " +
                    paymentPeriod.getStartingBalance().getCurrency()
                );
            }
            paymentItem.setAmount(amount);
        }

        // Update payee if provided
        if (payeeId != null || (payeeName != null && !payeeName.isBlank())) {
            Payee payee;
            if (payeeId != null) {
                payee = payeeService.findById(payeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + payeeId));
            } else {
                payee = payeeService.findOrCreatePayee(payeeName);
            }
            paymentItem.setPayee(payee);
        }

        // Update notes (can be null to clear notes)
        paymentItem.setNotes(notes);

        paymentItemRepository.save(paymentItem);

        // Explicitly recalculate balance before saving to ensure correctness
        paymentPeriod.calculateEndingBalance();

        paymentPeriodRepository.save(paymentPeriod);

        return paymentItem;
    }

    @Override
    public PaymentPeriod updatePaymentPeriodWithPaymentItems(
        Long id,
        LocalDate periodDate,
        MonetaryAmount startingBalance,
        List<PaymentItemData> paymentItems
    ) {
        if (periodDate == null) {
            throw new IllegalArgumentException("Period date cannot be null");
        }
        if (startingBalance == null) {
            throw new IllegalArgumentException("Starting balance cannot be null");
        }
        if (paymentItems == null) {
            throw new IllegalArgumentException("Payment items list cannot be null");
        }

        // Fetch payment period with items
        PaymentPeriod paymentPeriod = paymentPeriodRepository
            .findByIdWithPaymentItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment period not found: " + id));

        // Update basic properties
        if (!periodDate.equals(paymentPeriod.getPeriodDate())) {
            // Check if another period exists for the new date
            if (existsByPeriodDate(periodDate)) {
                throw new IllegalArgumentException(
                    "Payment period already exists for date: " + periodDate
                );
            }
            paymentPeriod.setPeriodDate(periodDate);
        }
        paymentPeriod.setStartingBalance(startingBalance);

        // Remove all existing payment items
        List<PaymentItem> existingItems = new ArrayList<>(paymentPeriod.getPaymentItems());
        for (PaymentItem item : existingItems) {
            paymentPeriod.removePaymentItem(item);
        }

        // Add new payment items
        int displayOrder = 0;
        for (PaymentItemData data : paymentItems) {
            // Validate currency matches
            if (!data.amount().getCurrency().equals(startingBalance.getCurrency())) {
                throw new IllegalArgumentException(
                    "Payment item currency must match payment period currency: " +
                    startingBalance.getCurrency() +
                    " (found: " + data.amount().getCurrency() + ")"
                );
            }

            // Resolve payee (find existing or create new)
            Payee payee;
            if (data.payeeId() != null) {
                payee = payeeService.findById(data.payeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + data.payeeId()));
            } else {
                payee = payeeService.findOrCreatePayee(data.payeeName());
            }

            PaymentItem paymentItem = new PaymentItem(data.amount(), payee, data.notes());
            paymentItem.setDisplayOrder(displayOrder++);
            paymentPeriod.addPaymentItem(paymentItem);
        }

        // Recalculate balance
        paymentPeriod.calculateEndingBalance();

        // Save (cascade will handle payment items)
        return paymentPeriodRepository.save(paymentPeriod);
    }

    @Override
    public void removePaymentItem(Long paymentPeriodId, Long paymentItemId) {
        PaymentPeriod paymentPeriod = paymentPeriodRepository
            .findByIdWithPaymentItems(paymentPeriodId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment period not found: " + paymentPeriodId
            ));

        PaymentItem paymentItem = paymentItemRepository.findById(paymentItemId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment item not found: " + paymentItemId
            ));

        // Verify the payment item belongs to this payment period
        if (!paymentItem.getPaymentPeriod().getId().equals(paymentPeriodId)) {
            throw new IllegalArgumentException(
                "Payment item does not belong to payment period: " + paymentPeriodId
            );
        }

        paymentPeriod.removePaymentItem(paymentItem);

        // Explicitly recalculate balance before saving to ensure correctness
        paymentPeriod.calculateEndingBalance();

        paymentPeriodRepository.save(paymentPeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPeriodDate(LocalDate periodDate) {
        return paymentPeriodRepository.existsByPeriodDate(periodDate);
    }
}
