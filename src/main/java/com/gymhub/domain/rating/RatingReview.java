package com.gymhub.domain.rating;

import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A rating + optional written review left by a customer for a {@link Provider}
 * (gym or specialist). Phase 2 plan — Section 25 (Ratings & Performance).
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>stars are constrained to the 1..5 range.</li>
 *   <li>A customer may leave only one review per provider (updates the existing one).</li>
 *   <li>Public rating summary only counts PUBLISHED reviews and (for the headline figure)
 *       only the last 6 months, per the business rule.</li>
 * </ul>
 */
@Entity
@Table(name = "rating_reviews", indexes = {
        @Index(name = "idx_rating_provider", columnList = "provider_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer (platform user) who wrote the review. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    /** The provider being rated (gym or specialist). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    /** Optional provider-service scope for service-level ratings (future). */
    @Column(name = "provider_service_id")
    private Long providerServiceId;

    @Column(nullable = false)
    private int stars;

    @Column(length = 1000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RatingStatus status = RatingStatus.PUBLISHED;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
