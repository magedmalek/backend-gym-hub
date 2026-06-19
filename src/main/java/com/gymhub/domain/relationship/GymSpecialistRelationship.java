package com.gymhub.domain.relationship;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.specialist.SpecialistProfile;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gym_specialist_relationships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymSpecialistRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parties ───────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_provider_id")
    private Provider gymProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_provider_id", nullable = false)
    private Provider specialistProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialist_profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    // ── Request metadata ──────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_provider_id")
    private Provider requestedByProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestInitiatorType requestInitiatorType;

    // ── Relationship type & status ────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RelationshipStatus status = RelationshipStatus.REQUESTED;

    private LocalDate startDate;
    private LocalDate endDate;

    // ── Commercial ownership (who collects payment / owns activation / service / package) ─

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommercialOwner paymentOwner = CommercialOwner.GYM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommercialOwner activationOwner = CommercialOwner.GYM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommercialOwner serviceOwner = CommercialOwner.GYM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CommercialOwner packageOwner = CommercialOwner.GYM;

    // ── Specialist selling rules ──────────────────────────────────────────────

    @Builder.Default
    private boolean canSpecialistSellToGymClients = false;

    @Builder.Default
    private boolean canSpecialistSellToNonGymClients = true;

    /** Subscriptions started before employment remain under their original owner. */
    @Builder.Default
    private boolean existingSubscriptionsRemainOriginalOwner = true;

    // ── Gym training pattern rules ────────────────────────────────────────────

    @Builder.Default
    private boolean allowGymTrainingPatterns = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GymPatternPolicy gymPatternPolicy = GymPatternPolicy.NOT_APPLICABLE;

    @Builder.Default
    private boolean mandatoryPatternsApply = false;

    @Builder.Default
    private boolean recommendedPatternsVisible = false;

    // ── Notes ─────────────────────────────────────────────────────────────────

    @Column(length = 2000)
    private String notes;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
