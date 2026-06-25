package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregated rating figures for a provider.
 * {@code averageStarsLast6Months} is the headline rating per the business rule;
 * {@code totalPublishedReviews} is the all-time published count.
 */
@Data
@Builder
public class RatingSummaryResponse {
    private Long providerId;
    private double averageStarsLast6Months;
    private long reviewsLast6Months;
    private long totalPublishedReviews;
}
