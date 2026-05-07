package com.gymhub.domain.freeze;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records a freeze period applied to a {@link Subscription}.
 *
 * Business rules enforced by the service layer:
 *  - startDate cannot be in the past.
 *  - duration (endDate - startDate) cannot exceed remaining freeze balance.
 *  - Applying a freeze extends the subscription endDate by the freeze duration.
 *  - Early cancellation is supported; upon cancel the unused days are credited back.
 */
@Entity
@Table(name = "subscription_freezes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionFreeze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** Actual date the freeze ended (either endDate or earlier if cancelled). */
    private LocalDate actualEndDate;

    /** Number of days consumed by this freeze record (set when completed/cancelled). */
    private Integer daysConsumed;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FreezeStatus status = FreezeStatus.ACTIVE;

    /** Employee who registered the freeze. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_employee_id")
    private Employee registeredBy;

    /** Employee who cancelled the freeze (null if not cancelled). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_employee_id")
    private Employee cancelledBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    // ── Helper ────────────────────────────────────────────────────────────────

    public int plannedDays() {
        return (int) startDate.until(endDate, java.time.temporal.ChronoUnit.DAYS);
    }
}
