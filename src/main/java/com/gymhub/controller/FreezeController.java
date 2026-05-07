package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.FreezeRequest;
import com.gymhub.dto.response.FreezeResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.FreezeService;
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
@RequestMapping("/api/v1/gyms/{gymId}/subscriptions/{subscriptionId}/freezes")
@RequiredArgsConstructor
@Tag(name = "Freeze", description = "Apply and manage subscription freeze periods")
public class FreezeController {

    private final FreezeService freezeService;

    @PostMapping
    @Operation(summary = "Apply a freeze to an active subscription — requires FREEZE_SUBSCRIPTION permission")
    public ResponseEntity<FreezeResponse> applyFreeze(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody FreezeRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(freezeService.applyFreeze(gymId, subscriptionId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "Get freeze history for a subscription")
    public ResponseEntity<PagedResponse<FreezeResponse>> getFreezeHistory(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(freezeService.getFreezeHistory(gymId, subscriptionId, currentUser, pageable)));
    }

    @PatchMapping("/{freezeId}/cancel")
    @Operation(summary = "Early-cancel an active freeze — credits unused days back to subscription")
    public ResponseEntity<FreezeResponse> cancelFreeze(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @PathVariable Long freezeId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(freezeService.cancelFreeze(gymId, subscriptionId, freezeId, currentUser));
    }
}
