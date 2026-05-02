package com.gymhub.domain.extraservice;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gymservice.GymService;
import com.gymhub.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks the use of a service that is INCLUDED in a subscription package.
 *
 * This is NOT a financial transaction — it is a usage log entry
 * that helps the gym monitor which bundled services a member is consuming.
 *
 * Contrast with {@link ExtraServiceTransaction} which is an independent sale.
 */
@Entity
@Table(name = "service_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private GymService service;

    /** Employee who recorded the usage. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_employee_id")
    private Employee recordedBy;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime usedAt;
}
