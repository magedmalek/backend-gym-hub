package com.gymhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Customer request to rate/review a provider. POST /api/v1/customer/reviews.
 */
@Data
public class RatingReviewRequest {

    @NotNull(message = "providerId is required")
    private Long providerId;

    /** Optional service-level scope. */
    private Long providerServiceId;

    @NotNull(message = "stars is required")
    @Min(value = 1, message = "stars must be between 1 and 5")
    @Max(value = 5, message = "stars must be between 1 and 5")
    private Integer stars;

    @Size(max = 1000)
    private String comment;
}
