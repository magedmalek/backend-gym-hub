package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.ComplaintRequest;
import com.gymhub.dto.request.ComplaintStatusUpdateRequest;
import com.gymhub.dto.response.ComplaintResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * FND-14 — complaints: customer-facing under /customer/complaints,
 * dashboard-facing under /gyms/{gymId}/complaints.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "File and resolve customer complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    // ── Customer ──────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/customer/complaints")
    @Operation(summary = "File a complaint (must reference a gym and/or provider)")
    public ResponseEntity<ComplaintResponse> file(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.fileComplaint(request, currentUser));
    }

    @GetMapping("/api/v1/customer/complaints")
    @Operation(summary = "List my complaints")
    public ResponseEntity<PagedResponse<ComplaintResponse>> myComplaints(
            @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                complaintService.getMyComplaints(currentUser, pageable)));
    }

    @PatchMapping("/api/v1/customer/complaints/{complaintId}/cancel")
    @Operation(summary = "Cancel (close) my own complaint while it is still open")
    public ResponseEntity<ComplaintResponse> cancel(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(complaintService.cancelMyComplaint(complaintId, currentUser));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/gyms/{gymId}/complaints")
    @Operation(summary = "List complaints routed to this gym (dashboard staff)")
    public ResponseEntity<PagedResponse<ComplaintResponse>> gymComplaints(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                complaintService.getGymComplaints(gymId, currentUser, pageable)));
    }

    @PatchMapping("/api/v1/gyms/{gymId}/complaints/{complaintId}/status")
    @Operation(summary = "Update a complaint's status / resolution note (dashboard staff)")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long gymId,
            @PathVariable Long complaintId,
            @Valid @RequestBody ComplaintStatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                complaintService.updateStatus(gymId, complaintId, request, currentUser));
    }
}
