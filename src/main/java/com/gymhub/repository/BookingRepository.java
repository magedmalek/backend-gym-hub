package com.gymhub.repository;

import com.gymhub.domain.session.Booking;
import com.gymhub.domain.session.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsBySessionIdAndCustomerIdAndStatus(Long sessionId, Long customerId, BookingStatus status);

    Optional<Booking> findByIdAndCustomerIdIn(Long id, List<Long> customerIds);

    Page<Booking> findByCustomerIdInOrderByBookedAtDesc(List<Long> customerIds, Pageable pageable);

    List<Booking> findBySessionIdOrderByBookedAtAsc(Long sessionId);
}
