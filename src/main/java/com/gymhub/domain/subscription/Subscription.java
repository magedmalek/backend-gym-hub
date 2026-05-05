package com.gymhub.domain.subscription;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gympackage.GymPackage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The actual subscription sold to a customer based on a {@link GymPackage}.
 *
 * Life-cycle:
 *   PENDING_PAYMENT → (payment received) → PENDING_ACTIVATION
 *                                        → (activation) → ACTIVE
 *                                                        → EXPIRED  (end date passed)
 *
 * If the gym allows deferred activation the start/end dates are set
 * at activation time, not at sale time — effectively delaying the
 * subscription start as requested by the business rules.
 *
 * Partial payment: if allowed by both the package and the gym settings,
 * paidAmount may be less than totalPrice while status = PENDING_PAYMENT.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relationships ────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private GymPackage gymPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    /** Employee who sold the subscription. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by_employee_id")
    private Employee soldBy;

    /** Employee who activated the subscription (may differ from the seller). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activated_by_employee_id")
    private Employee activatedBy;

    // ── Financials ───────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EGP";

    // ── Dates ────────────────────────────────────────────────────────────────

    /** Date the subscription was sold (always set at creation). */
    @Column(nullable = false)
    private LocalDate saleDate;

    /** Date the subscription was activated (null until activated). */
    private LocalDate activationDate;

    /**
     * First calendar day of validity.
     * Set at activation time; null if not yet activated.
     */
    private LocalDate startDate;

    /**
     * Last calendar day of validity.
     * Derived from startDate + package.durationDays at activation time.
     */
    private LocalDate endDate;

    // ── Invitation counter ───────────────────────────────────────────────────

    /** How many invitations have been used so far against this subscription. */
    @Builder.Default
    private int usedInvitations = 0;

    // ── Status ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING_PAYMENT;

    // ── Audit ────────────────────────────────────────────────────────────────

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public BigDecimal getRemainingAmount() {
        return totalPrice.subtract(paidAmount);
    }

    public boolean isFullyPaid() {
        return paidAmount.compareTo(totalPrice) >= 0;
    }

    public int getRemainingInvitations() {
        return Math.max(0, gymPackage.getMaxInvitations() - usedInvitations);
    }
}
