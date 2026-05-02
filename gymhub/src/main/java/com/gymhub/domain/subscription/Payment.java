package com.gymhub.domain.subscription;

import com.gymhub.domain.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a single payment instalment against a {@link Subscription}.
 *
 * Each time a customer pays (full or partial) a new Payment row is created.
 * The sum of all Payments for a subscription equals the paidAmount on the
 * Subscription entity (kept in sync by the service layer).
 *
 * This is intentionally kept separate from ExtraServiceTransaction which
 * covers paid add-on services outside the subscription.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EGP";

    /** The employee who received and recorded this payment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_employee_id")
    private Employee receivedBy;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime paidAt;
}
