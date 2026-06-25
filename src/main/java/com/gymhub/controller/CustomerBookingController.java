package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.response.BookingResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.SessionResponse;
import com.gymhub.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * CA-07 — customer-facing session discovery and booking.
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Bookings (Customer)", description = "Browse and book gym sessions (CA-07)")
public class CustomerBookingController {

    private final BookingService bookingService;

    @GetMapping("/gyms/{gymId}/sessions")
    @Operation(summary = "List upcoming scheduled sessions for a gym I belong to")
    public ResponseEntity<PagedResponse<SessionResponse>> upcomingSessions(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                bookingService.getUpcomingSessions(gymId, currentUser, pageable)));
    }

    @PostMapping("/sessions/{sessionId}/book")
    @Operation(summary = "Book a place in a session")
    public ResponseEntity<BookingResponse> book(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.book(sessionId, currentUser));
    }

    @GetMapping("/bookings")
    @Operation(summary = "List my bookings (newest first)")
    public ResponseEntity<PagedResponse<BookingResponse>> myBookings(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                bookingService.getMyBookings(currentUser, pageable)));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "Cancel one of my bookings")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId, currentUser));
    }
}
