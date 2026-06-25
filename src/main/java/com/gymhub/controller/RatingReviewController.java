package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.RatingReviewRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.RatingReviewResponse;
import com.gymhub.dto.response.RatingSummaryResponse;
import com.gymhub.service.RatingReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 2 plan — Section 25 (Ratings). Customer writes reviews; provider rating
 * data is publicly readable (paths whitelisted in SecurityConfig).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Ratings & Reviews", description = "Customer reviews and public provider rating summaries")
public class RatingReviewController {

    private final RatingReviewService ratingService;

    @PostMapping("/api/v1/customer/reviews")
    @Operation(summary = "Submit (or update) a review for a provider — one per customer per provider")
    public ResponseEntity<RatingReviewResponse> submitReview(
            @Valid @RequestBody RatingReviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.submitReview(request, currentUser));
    }

    @GetMapping("/api/v1/providers/{providerId}/reviews")
    @Operation(summary = "List published reviews for a provider (public)")
    public ResponseEntity<PagedResponse<RatingReviewResponse>> providerReviews(
            @PathVariable Long providerId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                ratingService.getPublishedReviews(providerId, pageable)));
    }

    @GetMapping("/api/v1/providers/{providerId}/rating-summary")
    @Operation(summary = "Average stars (last 6 months) and review counts for a provider (public)")
    public ResponseEntity<RatingSummaryResponse> ratingSummary(@PathVariable Long providerId) {
        return ResponseEntity.ok(ratingService.getRatingSummary(providerId));
    }
}
