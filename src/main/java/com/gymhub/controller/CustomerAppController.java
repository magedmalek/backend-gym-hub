package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.response.*;
import com.gymhub.service.CustomerAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer mobile app self-service APIs.
 *
 * All endpoints are scoped to the authenticated customer's own data.
 * No dashboard permission checks are applied here — the JWT must resolve to a User
 * who has the CUSTOMER role or an existing customer record.
 *
 * Business logic is fully shared with the dashboard — no duplication.
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer App", description = "Self-service APIs for the customer mobile application")
public class CustomerAppController {

    private final CustomerAppService customerAppService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerAppService.getMyProfile(currentUser));
    }

    // ── Gyms ──────────────────────────────────────────────────────────────────

    @GetMapping("/gyms")
    @Operation(summary = "List all gyms the customer is linked to")
    public ResponseEntity<List<GymResponse>> getMyGyms(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerAppService.getMyGyms(currentUser));
    }

    @GetMapping("/gyms/{gymId}")
    @Operation(summary = "Get public details for a specific gym")
    public ResponseEntity<GymResponse> getGymDetails(
            @PathVariable Long gymId) {
        return ResponseEntity.ok(customerAppService.getGymDetails(gymId));
    }

    // ── Gym link request ──────────────────────────────────────────────────────

    @PostMapping("/gyms/{gymId}/link-request")
    @Operation(summary = "Request to be linked as a customer of a gym")
    public ResponseEntity<GymLinkRequestResponse> requestGymLink(
            @PathVariable Long gymId,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerAppService.requestGymLink(gymId, notes, currentUser));
    }

    @GetMapping("/link-requests")
    @Operation(summary = "List all gym link requests made by this customer")
    public ResponseEntity<List<GymLinkRequestResponse>> getMyLinkRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerAppService.getMyLinkRequests(currentUser));
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    @GetMapping("/subscriptions")
    @Operation(summary = "List all subscriptions across all gyms for this customer")
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(customerAppService.getMySubscriptions(currentUser, pageable)));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Get a specific subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerAppService.getMySubscription(subscriptionId, currentUser));
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @GetMapping("/attendance")
    @Operation(summary = "Get this customer's attendance history across all gyms")
    public ResponseEntity<PagedResponse<AttendanceResponse>> getMyAttendance(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(customerAppService.getMyAttendance(currentUser, pageable)));
    }

    // ── Invitations ───────────────────────────────────────────────────────────

    @GetMapping("/invitations")
    @Operation(summary = "Get all invitations this customer has used (as host)")
    public ResponseEntity<PagedResponse<InvitationResponse>> getMyInvitations(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(customerAppService.getMyInvitations(currentUser, pageable)));
    }
}
