package com.gymhub.domain.extraservice;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.subscription.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records the sale of a paid add-on service outside of any subscription.
 *
 * This is a fully independent financial transaction — it is NEVER mixed with
 * the subscription invoice. The service sold must have
 * {@link GymService#isCanBeSoldIndependently()} = true.
 *
 * Examples: personal training session, locker rental, towel service
 * sold ad-hoc outside the member's current package.
 */
@Entity
@Table(name = "extra_service_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraServiceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private GymService service;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EGP";

    /** Phase 1: always CASH. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    /** Employee who sold the extra service. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_by_employee_id")
    private Employee soldBy;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime soldAt;
}
