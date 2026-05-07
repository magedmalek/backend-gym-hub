package com.gymhub.repository;

import com.gymhub.domain.freeze.FreezeStatus;
import com.gymhub.domain.freeze.SubscriptionFreeze;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionFreezeRepository extends JpaRepository<SubscriptionFreeze, Long> {

    Page<SubscriptionFreeze> findBySubscriptionId(Long subscriptionId, Pageable pageable);

    List<SubscriptionFreeze> findBySubscriptionIdAndStatus(Long subscriptionId, FreezeStatus status);

    Optional<SubscriptionFreeze> findByIdAndSubscriptionId(Long id, Long subscriptionId);

    /** Check for overlapping active freezes on the same subscription. */
    boolean existsBySubscriptionIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long subscriptionId, FreezeStatus status, LocalDate end, LocalDate start);
}
