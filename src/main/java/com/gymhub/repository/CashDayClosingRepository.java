package com.gymhub.repository;

import com.gymhub.domain.cashday.CashDayClosing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface CashDayClosingRepository extends JpaRepository<CashDayClosing, Long> {

    Page<CashDayClosing> findByGymIdOrderByBusinessDateDesc(Long gymId, Pageable pageable);

    boolean existsByGymIdAndBusinessDate(Long gymId, LocalDate businessDate);
}
