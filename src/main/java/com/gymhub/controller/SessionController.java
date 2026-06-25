package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.SessionCreateRequest;
import com.gymhub.dto.response.BookingResponse;
import com.gymhub.dto.response.SessionResponse;
import com.gymhub.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * GD-09 — dashboard management of gym sessions and their booking rosters.
 */
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions (Dashboard)", description = "Create and manage gym sessions / classes (GD-09)")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Create a session — requires MANAGE_SERVICES permission")
    public ResponseEntity<SessionResponse> create(
            @PathVariable Long gymId,
            @Valid @RequestBody SessionCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(gymId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "Calendar of sessions in a date range (defaults to next 30 days)")
    public ResponseEntity<List<SessionResponse>> calendar(
            @PathVariable Long gymId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sessionService.getCalendar(gymId, from, to, currentUser));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a single session")
    public ResponseEntity<SessionResponse> getOne(
            @PathVariable Long gymId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sessionService.getSession(gymId, sessionId, currentUser));
    }

    @PatchMapping("/{sessionId}/cancel")
    @Operation(summary = "Cancel a session — requires MANAGE_SERVICES permission")
    public ResponseEntity<SessionResponse> cancel(
            @PathVariable Long gymId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sessionService.cancelSession(gymId, sessionId, currentUser));
    }

    @GetMapping("/{sessionId}/bookings")
    @Operation(summary = "Get the booking roster for a session")
    public ResponseEntity<List<BookingResponse>> roster(
            @PathVariable Long gymId,
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sessionService.getRoster(gymId, sessionId, currentUser));
    }
}
