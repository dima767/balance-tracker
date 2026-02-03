package dk.balancetracker.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import javax.money.MonetaryAmount;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an individual payment/bill/expense line item within a payment period.
 * <p>
 * Each PaymentItem belongs to exactly one PaymentPeriod and represents a single
 * expense or payment that needs to be tracked.
 *
 * @author Dmitriy Kopylenko
 */
@Entity
@Table(name = "payment_items")
public class PaymentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Amount is required")
    @Column(name = "amount", nullable = false, length = 50)
    private MonetaryAmount amount;

    @NotNull(message = "Payee is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_id", nullable = false)
    private Payee payee;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "display_order")
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_period_id", nullable = false)
    private PaymentPeriod paymentPeriod;

    // Constructors

    protected PaymentItem() {
        // JPA requires a no-arg constructor
    }

    public PaymentItem(MonetaryAmount amount, Payee payee) {
        this.amount = amount;
        this.payee = payee;
    }

    public PaymentItem(MonetaryAmount amount, Payee payee, String notes) {
        this.amount = amount;
        this.payee = payee;
        this.notes = notes;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public MonetaryAmount getAmount() {
        return amount;
    }

    public void setAmount(MonetaryAmount amount) {
        this.amount = amount;
    }

    public Payee getPayee() {
        return payee;
    }

    public void setPayee(Payee payee) {
        this.payee = payee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public PaymentPeriod getPaymentPeriod() {
        return paymentPeriod;
    }

    /**
     * Package-private setter for bidirectional relationship management.
     * Use {@link PaymentPeriod#addPaymentItem(PaymentItem)} instead.
     */
    void setPaymentPeriod(PaymentPeriod paymentPeriod) {
        this.paymentPeriod = paymentPeriod;
    }

    // equals(), hashCode(), toString()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentItem that = (PaymentItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PaymentItem{" +
               "id=" + id +
               ", amount=" + amount +
               ", payee=" + (payee != null ? payee.getName() : "null") +
               ", notes='" + notes + '\'' +
               '}';
    }
}
