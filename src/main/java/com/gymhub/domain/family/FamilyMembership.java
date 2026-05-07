package com.gymhub.domain.family;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Links a sub-user (family member) to a main customer's family-package subscription.
 *
 * Business rules:
 *  - The package must have isFamilyPackage = true.
 *  - The number of FamilyMembership rows per subscription cannot exceed
 *    {@link com.gymhub.domain.gympackage.GymPackage#getMaxSubUsers()}.
 *  - Each sub-user must have their own independent Customer record.
 *  - The same sub-user cannot be added twice to the same subscription.
 */
@Entity
@Table(name = "family_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "sub_customer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The main subscriber who owns the family package. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_customer_id", nullable = false)
    private Customer mainCustomer;

    /** The sub-user being attached to the family package. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_customer_id", nullable = false)
    private Customer subCustomer;

    /** The family-package subscription this membership belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /** Free-text relation label (e.g. "Spouse", "Child"). Optional. */
    @Column(length = 100)
    private String relationType;

    /** Employee who created this membership link. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private Employee createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
