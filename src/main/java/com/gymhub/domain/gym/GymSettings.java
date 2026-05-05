package com.gymhub.domain.gym;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Operational policies that each gym configures independently.
 * These settings influence how subscriptions, payments, and entry are handled.
 */
@Entity
@Table(name = "gym_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false, unique = true)
    private Gym gym;

    /**
     * Whether the gym permits partial (installment) payments for subscriptions
     * at the gym level. Individual packages can override this per-package.
     */
    @Builder.Default
    private boolean allowPartialPayment = false;

    /**
     * How a fully-paid subscription transitions to ACTIVE status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ActivationPolicy activationPolicy = ActivationPolicy.IMMEDIATE;

    /**
     * Which entry methods are enabled for this gym.
     * (Fingerprint is excluded from the current scope.)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "gym_entrance_methods",
            joinColumns = @JoinColumn(name = "gym_settings_id"))
    @Column(name = "entrance_method")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<EntranceMethod> enabledEntranceMethods = new HashSet<>();
}
