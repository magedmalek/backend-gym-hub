package com.gymhub.controller;

import com.gymhub.domain.gym.GymStatus;
import com.gymhub.dto.request.CreateGymRequest;
import com.gymhub.dto.request.GymSettingsRequest;
import com.gymhub.dto.response.GymLinkRequestResponse;
import com.gymhub.dto.response.GymResponse;
import com.gymhub.domain.user.User;
import com.gymhub.service.GymManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Management", description = "Create and manage gym entities and their settings")
public class GymController {

    private final GymManagementService gymService;

    @PostMapping
    @Operation(summary = "Create a new gym (authenticated user becomes the owner)")
    public ResponseEntity<GymResponse> createGym(
            @Valid @RequestBody CreateGymRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gymService.createGym(request, currentUser.getEmail()));
    }

    @GetMapping("/{gymId}")
    @Operation(summary = "Get gym details")
    public ResponseEntity<GymResponse> getGym(@PathVariable Long gymId) {
        return ResponseEntity.ok(gymService.getGym(gymId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all gyms owned by the authenticated user")
    public ResponseEntity<List<GymResponse>> getMyGyms(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(gymService.getMyGyms(currentUser.getEmail()));
    }

    @PutMapping("/{gymId}")
    @Operation(summary = "Update gym profile information")
    public ResponseEntity<GymResponse> updateGym(
            @PathVariable Long gymId,
            @Valid @RequestBody CreateGymRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(gymService.updateGym(gymId, request, currentUser.getEmail()));
    }

    @PutMapping("/{gymId}/settings")
    @Operation(summary = "Update gym operational settings (activation policy, payment, entry methods)")
    public ResponseEntity<Void> updateSettings(
            @PathVariable Long gymId,
            @Valid @RequestBody GymSettingsRequest request,
            @AuthenticationPrincipal User currentUser) {
        gymService.updateSettings(gymId, request, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{gymId}/status")
    @Operation(summary = "Change gym status (ACTIVE / INACTIVE / SUSPENDED)")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long gymId,
            @RequestParam GymStatus status,
            @AuthenticationPrincipal User currentUser) {
        gymService.updateStatus(gymId, status, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    // ── Customer link requests ────────────────────────────────────────────────

    @GetMapping("/{gymId}/link-requests")
    @Operation(summary = "List pending customer link requests for this gym")
    public ResponseEntity<List<GymLinkRequestResponse>> getPendingLinkRequests(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(gymService.getPendingLinkRequests(gymId, currentUser.getEmail()));
    }

    @PostMapping("/{gymId}/link-requests/{requestId}/approve")
    @Operation(summary = "Approve a customer link request — creates Customer record")
    public ResponseEntity<Void> approveLinkRequest(
            @PathVariable Long gymId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal User currentUser) {
        gymService.approveLinkRequest(gymId, requestId, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/link-requests/{requestId}/reject")
    @Operation(summary = "Reject a customer link request")
    public ResponseEntity<Void> rejectLinkRequest(
            @PathVariable Long gymId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal User currentUser) {
        gymService.rejectLinkRequest(gymId, requestId, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }
}
