package com.gymhub.repository;

import com.gymhub.domain.subscription.Payment;
import com.gymhub.domain.subscription.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionId(Long subscriptionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.subscription.id = :subId")
    BigDecimal sumAmountBySubscriptionId(@Param("subId") Long subscriptionId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.subscription.gym.id = :gymId AND p.paymentMethod = :method " +
            "AND p.paidAt >= :start AND p.paidAt < :end")
    BigDecimal sumByGymAndMethodBetween(@Param("gymId") Long gymId,
                                        @Param("method") PaymentMethod method,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p " +
            "WHERE p.subscription.gym.id = :gymId AND p.paymentMethod = :method " +
            "AND p.paidAt >= :start AND p.paidAt < :end")
    long countByGymAndMethodBetween(@Param("gymId") Long gymId,
                                    @Param("method") PaymentMethod method,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.subscription.gym.id = :gymId " +
            "AND p.paidAt >= :start AND p.paidAt < :end")
    BigDecimal sumByGymBetween(@Param("gymId") Long gymId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
}
