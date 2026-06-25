package com.gymhub.service;

import com.gymhub.domain.provider.Provider;
import com.gymhub.domain.rating.RatingReview;
import com.gymhub.domain.rating.RatingStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.RatingReviewRequest;
import com.gymhub.dto.response.RatingReviewResponse;
import com.gymhub.dto.response.RatingSummaryResponse;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.ProviderRepository;
import com.gymhub.repository.RatingReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Phase 2 plan — Section 25 (Ratings & Performance).
 * A customer rates a provider; one review per (customer, provider) — re-submitting updates it.
 * Public summaries surface the last-6-months average per the business rule.
 */
@Service
@RequiredArgsConstructor
public class RatingReviewService {

    private final RatingReviewRepository ratingRepository;
    private final ProviderRepository providerRepository;

    // ── Customer creates / updates a review ───────────────────────────────────

    @Transactional
    public RatingReviewResponse submitReview(RatingReviewRequest request, User currentUser) {
        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));

        RatingReview review = ratingRepository
                .findByProviderIdAndReviewerId(provider.getId(), currentUser.getId())
                .orElseGet(() -> RatingReview.builder()
                        .reviewer(currentUser)
                        .provider(provider)
                        .status(RatingStatus.PUBLISHED)
                        .build());

        review.setStars(request.getStars());
        review.setComment(request.getComment());
        review.setProviderServiceId(request.getProviderServiceId());
        if (review.getId() != null) {
            review.setUpdatedAt(LocalDateTime.now());
        }

        return toResponse(ratingRepository.save(review));
    }

    // ── Public read ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<RatingReviewResponse> getPublishedReviews(Long providerId, Pageable pageable) {
        requireProvider(providerId);
        return ratingRepository
                .findByProviderIdAndStatusOrderByCreatedAtDesc(providerId, RatingStatus.PUBLISHED, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long providerId) {
        requireProvider(providerId);
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        double avg = ratingRepository.averageStarsSince(providerId, RatingStatus.PUBLISHED, sixMonthsAgo);
        long recent = ratingRepository.countByProviderIdAndStatusAndCreatedAtGreaterThanEqual(
                providerId, RatingStatus.PUBLISHED, sixMonthsAgo);
        long total = ratingRepository.countByProviderIdAndStatus(providerId, RatingStatus.PUBLISHED);
        return RatingSummaryResponse.builder()
                .providerId(providerId)
                .averageStarsLast6Months(Math.round(avg * 100.0) / 100.0)
                .reviewsLast6Months(recent)
                .totalPublishedReviews(total)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireProvider(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Provider", providerId);
        }
    }

    private RatingReviewResponse toResponse(RatingReview r) {
        return RatingReviewResponse.builder()
                .id(r.getId())
                .providerId(r.getProvider().getId())
                .providerServiceId(r.getProviderServiceId())
                .reviewerName(r.getReviewer() != null ? r.getReviewer().getFullName() : null)
                .stars(r.getStars())
                .comment(r.getComment())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
