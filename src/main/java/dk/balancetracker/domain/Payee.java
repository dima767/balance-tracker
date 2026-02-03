package dk.balancetracker.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a payee reference data entity.
 * <p>
 * Payees are the recipients of payments (e.g., "Electric Company", "Landlord", "Insurance Co").
 * This is reference data that can be reused across multiple payment items.
 *
 * @author Dmitriy Kopylenko
 */
@Entity
@Table(name = "payees")
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Payee name is required")
    @Size(max = 200, message = "Payee name must not exceed 200 characters")
    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @OneToMany(mappedBy = "payee", fetch = FetchType.LAZY)
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors

    protected Payee() {
        // JPA requires a no-arg constructor
    }

    public Payee(String name) {
        this.name = name;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        Payee payee = (Payee) o;
        return Objects.equals(id, payee.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Payee{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}
