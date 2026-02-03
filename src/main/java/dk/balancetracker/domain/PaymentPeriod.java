package dk.balancetracker.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a payment period for tracking account balances and payments.
 * <p>
 * A payment period tracks the starting balance, all payment items for that period,
 * and calculates the ending balance after all payments are accounted for.
 *
 * @author Dmitriy Kopylenko
 */
@Entity
@Table(name = "payment_periods")
public class PaymentPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Period date is required")
    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @NotNull(message = "Starting balance is required")
    @Column(name = "starting_balance", nullable = false, length = 50)
    private MonetaryAmount startingBalance;

    @Column(name = "ending_balance", length = 50)
    private MonetaryAmount endingBalance;

    @OneToMany(
        mappedBy = "paymentPeriod",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC, id ASC")
    private List<PaymentItem> paymentItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle callbacks

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        calculateEndingBalance();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateEndingBalance();
    }

    // Constructors

    protected PaymentPeriod() {
        // JPA requires a no-arg constructor
    }

    public PaymentPeriod(LocalDate periodDate, MonetaryAmount startingBalance) {
        this.periodDate = periodDate;
        this.startingBalance = startingBalance;
    }

    // Business logic

    /**
     * Calculates the ending balance based on starting balance and all payment items.
     * <p>
     * Formula: endingBalance = startingBalance - sum(paymentItems.amount)
     */
    public void calculateEndingBalance() {
        if (startingBalance == null) {
            endingBalance = null;
            return;
        }

        MonetaryAmount total = startingBalance;
        for (PaymentItem item : paymentItems) {
            if (item.getAmount() != null) {
                total = total.subtract(item.getAmount());
            }
        }
        endingBalance = total;
    }

    /**
     * Adds a payment item to this payment period.
     * Manages the bidirectional relationship properly.
     *
     * @param paymentItem the payment item to add
     */
    public void addPaymentItem(PaymentItem paymentItem) {
        if (paymentItem == null) {
            throw new IllegalArgumentException("PaymentItem cannot be null");
        }
        paymentItems.add(paymentItem);
        paymentItem.setPaymentPeriod(this);
    }

    /**
     * Removes a payment item from this payment period.
     * Manages the bidirectional relationship properly.
     *
     * @param paymentItem the payment item to remove
     */
    public void removePaymentItem(PaymentItem paymentItem) {
        if (paymentItem == null) {
            return;
        }
        paymentItems.remove(paymentItem);
        paymentItem.setPaymentPeriod(null);
    }

    /**
     * Returns the total amount of all payment items.
     *
     * @return total payment amount, or zero in the starting balance currency if no items
     */
    public MonetaryAmount getTotalPayments() {
        if (paymentItems.isEmpty()) {
            return Money.of(0, startingBalance.getCurrency());
        }

        MonetaryAmount total = Money.of(0, startingBalance.getCurrency());
        for (PaymentItem item : paymentItems) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
            }
        }
        return total;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public LocalDate getPeriodDate() {
        return periodDate;
    }

    public void setPeriodDate(LocalDate periodDate) {
        this.periodDate = periodDate;
    }

    public MonetaryAmount getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(MonetaryAmount startingBalance) {
        this.startingBalance = startingBalance;
    }

    public MonetaryAmount getEndingBalance() {
        return endingBalance;
    }

    public List<PaymentItem> getPaymentItems() {
        return new ArrayList<>(paymentItems);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // equals(), hashCode(), toString()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentPeriod that = (PaymentPeriod) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PaymentPeriod{" +
               "id=" + id +
               ", periodDate=" + periodDate +
               ", startingBalance=" + startingBalance +
               ", endingBalance=" + endingBalance +
               ", paymentItemsCount=" + paymentItems.size() +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}
