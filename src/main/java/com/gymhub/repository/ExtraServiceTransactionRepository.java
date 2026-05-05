package com.gymhub.repository;

import com.gymhub.domain.extraservice.ExtraServiceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface ExtraServiceTransactionRepository extends JpaRepository<ExtraServiceTransaction, Long> {

    Page<ExtraServiceTransaction> findByCustomerIdOrderBySoldAtDesc(Long customerId, Pageable pageable);

    Page<ExtraServiceTransaction> findByGymIdOrderBySoldAtDesc(Long gymId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM ExtraServiceTransaction t " +
           "WHERE t.gym.id = :gymId AND t.soldAt BETWEEN :from AND :to")
    BigDecimal sumRevenueByGymIdBetween(@Param("gymId") Long gymId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
