package com.gymhub.repository;

import com.gymhub.domain.rating.RatingReview;
import com.gymhub.domain.rating.RatingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RatingReviewRepository extends JpaRepository<RatingReview, Long> {

    Page<RatingReview> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Long providerId, RatingStatus status, Pageable pageable);

    Optional<RatingReview> findByProviderIdAndReviewerId(Long providerId, Long reviewerId);

    @Query("select coalesce(avg(r.stars), 0) from RatingReview r " +
            "where r.provider.id = :providerId and r.status = :status and r.createdAt >= :since")
    double averageStarsSince(@Param("providerId") Long providerId,
                             @Param("status") RatingStatus status,
                             @Param("since") LocalDateTime since);

    long countByProviderIdAndStatusAndCreatedAtGreaterThanEqual(
            Long providerId, RatingStatus status, LocalDateTime since);

    long countByProviderIdAndStatus(Long providerId, RatingStatus status);
}
