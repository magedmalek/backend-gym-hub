package com.gymhub.repository;

import com.gymhub.domain.attendance.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Page<Attendance> findByCustomerIdOrderByVisitedAtDesc(Long customerId, Pageable pageable);

    Page<Attendance> findByCustomerIdInOrderByVisitedAtDesc(java.util.List<Long> customerIds, Pageable pageable);

    Page<Attendance> findByGymIdOrderByVisitedAtDesc(Long gymId, Pageable pageable);

    List<Attendance> findByCustomerIdAndVisitedAtBetween(Long customerId,
                                                          LocalDateTime from,
                                                          LocalDateTime to);

    long countByGymIdAndVisitedAtBetween(Long gymId, LocalDateTime from, LocalDateTime to);
}
