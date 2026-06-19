package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.relationship.CreateGymRelationshipRequest;
import com.gymhub.dto.request.relationship.CreateSpecialistRelationshipRequest;
import com.gymhub.dto.request.relationship.UpdateRelationshipPolicyRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.relationship.GymSpecialistRelationshipResponse;
import com.gymhub.service.GymSpecialistRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Specialist Relationships", description = "Gym ↔ specialist relationship contract management")
public class GymSpecialistRelationshipController {

    private final GymSpecialistRelationshipService relationshipService;

    // ── Gym-initiated request ─────────────────────────────────────────────────

    @PostMapping("/api/v1/gyms/{gymId}/specialist-relationships/requests")
    @Operation(summary = "Gym sends a relationship request to a specialist")
    public ResponseEntity<GymSpecialistRelationshipResponse> requestFromGym(
            @PathVariable Long gymId,
            @Valid @RequestBody CreateGymRelationshipRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(relationshipService.requestFromGym(gymId, request, currentUser));
    }

    @GetMapping("/api/v1/gyms/{gymId}/specialist-relationships")
    @Operation(summary = "List all specialist relationships for a gym")
    public ResponseEntity<PagedResponse<GymSpecialistRelationshipResponse>> getGymRelationships(
            @PathVariable Long gymId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(PagedResponse.from(
                relationshipService.getGymRelationships(gymId, currentUser, PageRequest.of(page, size))));
    }

    // ── Specialist-initiated request ──────────────────────────────────────────

    @PostMapping("/api/v1/specialists/me/gym-relationships/requests")
    @Operation(summary = "Specialist sends a relationship request to a gym")
    public ResponseEntity<GymSpecialistRelationshipResponse> requestFromSpecialist(
            @Valid @RequestBody CreateSpecialistRelationshipRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(relationshipService.requestFromSpecialist(request, currentUser));
    }

    @GetMapping("/api/v1/specialists/me/gym-relationships")
    @Operation(summary = "List all gym relationships for the authenticated specialist")
    public ResponseEntity<PagedResponse<GymSpecialistRelationshipResponse>> getSpecialistRelationships(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(PagedResponse.from(
                relationshipService.getSpecialistRelationships(currentUser, PageRequest.of(page, size))));
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @PostMapping("/api/v1/specialist-relationships/{relationshipId}/accept")
    @Operation(summary = "Accept a pending relationship request (target side only)")
    public ResponseEntity<GymSpecialistRelationshipResponse> accept(
            @PathVariable Long relationshipId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(relationshipService.accept(relationshipId, currentUser));
    }

    @PostMapping("/api/v1/specialist-relationships/{relationshipId}/reject")
    @Operation(summary = "Reject a pending relationship request (target side only)")
    public ResponseEntity<GymSpecialistRelationshipResponse> reject(
            @PathVariable Long relationshipId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(relationshipService.reject(relationshipId, currentUser));
    }

    @PostMapping("/api/v1/specialist-relationships/{relationshipId}/suspend")
    @Operation(summary = "Suspend an active relationship (either participant)")
    public ResponseEntity<GymSpecialistRelationshipResponse> suspend(
            @PathVariable Long relationshipId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(relationshipService.suspend(relationshipId, currentUser));
    }

    @PostMapping("/api/v1/specialist-relationships/{relationshipId}/terminate")
    @Operation(summary = "Terminate a relationship (either participant)")
    public ResponseEntity<GymSpecialistRelationshipResponse> terminate(
            @PathVariable Long relationshipId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(relationshipService.terminate(relationshipId, currentUser));
    }

    @PutMapping("/api/v1/specialist-relationships/{relationshipId}/policy")
    @Operation(summary = "Update commercial ownership and pattern policy for a relationship")
    public ResponseEntity<GymSpecialistRelationshipResponse> updatePolicy(
            @PathVariable Long relationshipId,
            @Valid @RequestBody UpdateRelationshipPolicyRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(relationshipService.updatePolicy(relationshipId, request, currentUser));
    }
}
