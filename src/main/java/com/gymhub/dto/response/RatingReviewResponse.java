package com.gymhub.dto.response;

import com.gymhub.domain.rating.RatingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RatingReviewResponse {
    private Long id;
    private Long providerId;
    private Long providerServiceId;
    private String reviewerName;
    private int stars;
    private String comment;
    private RatingStatus status;
    private LocalDateTime createdAt;
}
