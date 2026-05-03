package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.UseInvitationRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Register guest invitations consumed from a member's subscription quota")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @Operation(summary = "Use one invitation slot — acting employee resolved from JWT")
    public ResponseEntity<?> useInvitation(
            @PathVariable Long gymId,
            @Valid @RequestBody UseInvitationRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.useInvitation(gymId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List all invitations for this gym")
    public ResponseEntity<PagedResponse<?>> getByGym(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(invitationService.getByGym(gymId, currentUser, pageable)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List invitations where the specified customer is the host (inviter)")
    public ResponseEntity<PagedResponse<?>> getByHost(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(invitationService.getByHost(gymId, customerId, currentUser, pageable)));
    }
}
