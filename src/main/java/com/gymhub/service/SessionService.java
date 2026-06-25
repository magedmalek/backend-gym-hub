package com.gymhub.service;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.employee.EmployeePermission;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.session.Booking;
import com.gymhub.domain.session.GymSession;
import com.gymhub.domain.session.SessionStatus;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.SessionCreateRequest;
import com.gymhub.dto.response.BookingResponse;
import com.gymhub.dto.response.SessionResponse;
import com.gymhub.exception.BusinessException;
import com.gymhub.exception.ResourceNotFoundException;
import com.gymhub.repository.BookingRepository;
import com.gymhub.repository.GymSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GD-09 Calendar, Sessions & Bookings — dashboard (gym-side) management of sessions.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final GymSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final GymAccessService gymAccessService;

    // ── Manage ────────────────────────────────────────────────────────────────

    @Transactional
    public SessionResponse createSession(Long gymId, SessionCreateRequest request, User currentUser) {
        Employee emp = gymAccessService.resolveActingEmployee(
                currentUser, gymId, EmployeePermission.MANAGE_SERVICES);
        Gym gym = gymAccessService.requireGym(gymId);

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("Session end time must be after start time");
        }

        GymSession session = GymSession.builder()
                .gym(gym)
                .title(request.getTitle())
                .description(request.getDescription())
                .instructorName(request.getInstructorName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .bookedCount(0)
                .status(SessionStatus.SCHEDULED)
                .createdBy(emp)
                .build();
        return toResponse(sessionRepository.save(session));
    }

    @Transactional
    public SessionResponse cancelSession(Long gymId, Long sessionId, User currentUser) {
        gymAccessService.resolveActingEmployee(currentUser, gymId, EmployeePermission.MANAGE_SERVICES);
        GymSession session = requireSession(gymId, sessionId);
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException("Session is already cancelled");
        }
        session.setStatus(SessionStatus.CANCELLED);
        session.setUpdatedAt(LocalDateTime.now());
        return toResponse(sessionRepository.save(session));
    }

    // ── Calendar / read ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SessionResponse> getCalendar(Long gymId, LocalDate from, LocalDate to, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(30);
        return sessionRepository.findByGymIdAndStartTimeBetweenOrderByStartTimeAsc(
                        gymId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long gymId, Long sessionId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        return toResponse(requireSession(gymId, sessionId));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getRoster(Long gymId, Long sessionId, User currentUser) {
        gymAccessService.assertDashboardAccess(currentUser, gymId);
        requireSession(gymId, sessionId);
        return bookingRepository.findBySessionIdOrderByBookedAtAsc(sessionId)
                .stream().map(SessionService::toBookingResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GymSession requireSession(Long gymId, Long sessionId) {
        return sessionRepository.findByIdAndGymId(sessionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
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

    static BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .sessionId(b.getSession().getId())
                .sessionTitle(b.getSession().getTitle())
                .sessionStartTime(b.getSession().getStartTime())
                .customerId(b.getCustomer().getId())
                .customerName(b.getCustomer().getUser() != null
                        ? b.getCustomer().getUser().getFullName() : null)
                .status(b.getStatus())
                .bookedAt(b.getBookedAt())
                .cancelledAt(b.getCancelledAt())
                .build();
    }
}
