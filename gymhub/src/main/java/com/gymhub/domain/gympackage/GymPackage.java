package com.gymhub.domain.gympackage;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gymservice.GymService;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A subscription package that the gym sells to its members.
 *
 * The package defines:
 *  - duration and price
 *  - which services are bundled inside
 *  - the invitation quota and guest-repeat rules
 *  - whether partial payment is allowed for this specific package
 *
 * Selling the package to a customer creates a {@link com.gymhub.domain.subscription.Subscription}.
 */
@Entity
@Table(name = "gym_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    // ── Pricing & duration ──────────────────────────────────────────────────

    /** Duration in calendar days. */
    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "EGP";

    // ── Invitation rules ────────────────────────────────────────────────────

    /** Maximum number of guest invitations a subscriber can use. 0 = no invitations. */
    @Builder.Default
    private int maxInvitations = 0;

    /**
     * If true the same guest can be brought more than once under this package.
     * If false each guest may only be invited once for the life of the subscription.
     */
    @Builder.Default
    private boolean allowGuestRepeatVisit = false;

    // ── Payment policy ──────────────────────────────────────────────────────

    /**
     * Per-package override for partial (installment) payment.
     * Defaults to false; set to true to allow split payment for subscribers of this package.
     */
    @Builder.Default
    private boolean allowPartialPayment = false;

    // ── Included services ───────────────────────────────────────────────────

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "package_services",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<GymService> includedServices = new HashSet<>();

    // ── Status ───────────────────────────────────────────────────────────────

    @Builder.Default
    private boolean active = true;

    // ── Audit ────────────────────────────────────────────────────────────────

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
