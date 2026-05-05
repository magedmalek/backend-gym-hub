package com.gymhub.repository;

import com.gymhub.domain.subscription.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionId(Long subscriptionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.subscription.id = :subId")
    BigDecimal sumAmountBySubscriptionId(@Param("subId") Long subscriptionId);
}
