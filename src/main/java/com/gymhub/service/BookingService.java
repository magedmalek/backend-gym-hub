package com.gymhub.service;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.session.Booking;
import com.gymhub.domain.session.BookingStatus;
import com.gymhub.domain.session.GymSession;
import com.gymhub.domain.session.SessionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.response.BookingResponse;
import com.gymhub.dto.response.SessionResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.exception.UnauthorizedException;
import com.gymhub.repository.BookingRepository;
import com.gymhub.repository.CustomerRepository;
import com.gymhub.repository.GymSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CA-07 Customer Booking Calendar — customer-side discovery and booking of gym sessions.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final GymSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;

    // ── Discovery ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SessionResponse> getUpcomingSessions(Long gymId, User currentUser, Pageable pageable) {
        requireMembership(currentUser, gymId);
        return sessionRepository
                .findByGymIdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                        gymId, SessionStatus.SCHEDULED, LocalDateTime.now(), pageable)
                .map(this::toResponse);
    }

    // ── Book / cancel ─────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse book(Long sessionId, User currentUser) {
        GymSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        Customer customer = requireMembership(currentUser, session.getGym().getId());

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled sessions can be booked");
        }
        if (!session.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("This session has already started");
        }
        if (!session.hasFreeSlot()) {
            throw new BusinessException("This session is fully booked");
        }
        if (bookingRepository.existsBySessionIdAndCustomerIdAndStatus(
                sessionId, customer.getId(), BookingStatus.CONFIRMED)) {
            throw new BusinessException("You already have a confirmed booking for this session");
        }

        session.setBookedCount(session.getBookedCount() + 1);
        sessionRepository.save(session);

        Booking booking = Booking.builder()
                .session(session)
                .customer(customer)
                .status(BookingStatus.CONFIRMED)
                .build();
        return SessionService.toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, User currentUser) {
        List<Long> customerIds = myCustomerIds(currentUser);
        Booking booking = bookingRepository.findByIdAndCustomerIdIn(bookingId, customerIds)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled");
        }

        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        GymSession session = booking.getSession();
        if (wasConfirmed && session.getBookedCount() > 0) {
            session.setBookedCount(session.getBookedCount() - 1);
            sessionRepository.save(session);
        }
        return SessionService.toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(User currentUser, Pageable pageable) {
        List<Long> customerIds = myCustomerIds(currentUser);
        return bookingRepository.findByCustomerIdInOrderByBookedAtDesc(customerIds, pageable)
                .map(SessionService::toBookingResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Customer requireMembership(User user, Long gymId) {
        return customerRepository.findByUserIdAndGymId(user.getId(), gymId)
                .orElseThrow(() -> new UnauthorizedException(
                        "You must be a member of this gym to view or book its sessions"));
    }

    private List<Long> myCustomerIds(User user) {
        List<Long> ids = customerRepository.findByUserId(user.getId())
                .stream().map(Customer::getId).toList();
        if (ids.isEmpty()) {
            throw new UnauthorizedException("No customer profile found for this account");
        }
        return ids;
    }

    private SessionResponse toResponse(GymSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .gymId(s.getGym().getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .instructorName(s.getInstructorName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .capacity(s.getCapacity())
                .bookedCount(s.getBookedCount())
                .availableSlots(Math.max(0, s.getCapacity() - s.getBookedCount()))
                .status(s.getStatus())
                .build();
    }
}
