package com.gymhub.repository;

import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.subscription.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Page<Subscription> findByCustomerId(Long customerId, Pageable pageable);

    Page<Subscription> findByCustomerIdIn(List<Long> customerIds, Pageable pageable);

    Page<Subscription> findByGymId(Long gymId, Pageable pageable);

    List<Subscription> findByCustomerIdAndStatus(Long customerId, SubscriptionStatus status);

    Optional<Subscription> findByIdAndGymId(Long id, Long gymId);

    /** Find active subscriptions that have expired (end date in the past). */
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.status = 'ACTIVE' AND s.endDate < :today")
    List<Subscription> findExpiredActive(@Param("today") LocalDate today);

    /** Count active subscriptions per gym for reporting. */
    long countByGymIdAndStatus(Long gymId, SubscriptionStatus status);

    /** Count subscriptions sold within a date range (GD-15 reporting). */
    long countByGymIdAndSaleDateBetween(Long gymId, LocalDate from, LocalDate to);
}
