package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.AddSubUserRequest;
import com.gymhub.dto.response.FamilyMemberResponse;
import com.gymhub.service.FamilyMembershipService;
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
@RequestMapping("/api/v1/gyms/{gymId}/subscriptions/{subscriptionId}/sub-users")
@RequiredArgsConstructor
@Tag(name = "Family Package", description = "Manage sub-users on family package subscriptions")
public class FamilyMembershipController {

    private final FamilyMembershipService familyMembershipService;

    @PostMapping
    @Operation(summary = "Add a sub-user to a family package subscription — requires MANAGE_CUSTOMERS")
    public ResponseEntity<FamilyMemberResponse> addSubUser(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody AddSubUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyMembershipService.addSubUser(gymId, subscriptionId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List all sub-users on a family package subscription")
    public ResponseEntity<List<FamilyMemberResponse>> getSubUsers(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(familyMembershipService.getSubUsers(gymId, subscriptionId, currentUser));
    }

    @DeleteMapping("/{subCustomerId}")
    @Operation(summary = "Remove a sub-user from a family package subscription")
    public ResponseEntity<Void> removeSubUser(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @PathVariable Long subCustomerId,
            @AuthenticationPrincipal User currentUser) {
        familyMembershipService.removeSubUser(gymId, subscriptionId, subCustomerId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
