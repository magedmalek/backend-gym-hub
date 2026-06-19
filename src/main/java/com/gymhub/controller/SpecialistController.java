package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.specialist.CreateSpecialistProfileRequest;
import com.gymhub.dto.request.specialist.UpdateEnabledToolsRequest;
import com.gymhub.dto.request.specialist.UpdateServiceAxesRequest;
import com.gymhub.dto.response.specialist.EnabledToolResponse;
import com.gymhub.dto.response.specialist.ServiceAxisSelectionResponse;
import com.gymhub.dto.response.specialist.SpecialistProfileResponse;
import com.gymhub.service.SpecialistService;
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
@RequestMapping("/api/v1/specialists")
@RequiredArgsConstructor
@Tag(name = "Specialists", description = "Specialist profile, service axes, and tool management")
public class SpecialistController {

    private final SpecialistService specialistService;

    // ── Profile ───────────────────────────────────────────────────────────────

    @PostMapping("/me/profile")
    @Operation(summary = "Create specialist profile for the authenticated service provider")
    public ResponseEntity<SpecialistProfileResponse> createProfile(
            @Valid @RequestBody CreateSpecialistProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(specialistService.createProfile(request, currentUser));
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Get own specialist profile")
    public ResponseEntity<SpecialistProfileResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.getMyProfile(currentUser));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update own specialist profile")
    public ResponseEntity<SpecialistProfileResponse> updateMyProfile(
            @Valid @RequestBody CreateSpecialistProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.updateMyProfile(request, currentUser));
    }

    @GetMapping("/{specialistId}/profile")
    @Operation(summary = "Get public specialist profile by ID")
    public ResponseEntity<SpecialistProfileResponse> getPublicProfile(
            @PathVariable Long specialistId) {
        return ResponseEntity.ok(specialistService.getPublicProfile(specialistId));
    }

    // ── Service axes ──────────────────────────────────────────────────────────

    @PutMapping("/me/service-axes")
    @Operation(summary = "Update service axes — auto-enables matching tools")
    public ResponseEntity<List<ServiceAxisSelectionResponse>> updateServiceAxes(
            @Valid @RequestBody UpdateServiceAxesRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.updateServiceAxes(request, currentUser));
    }

    @GetMapping("/me/service-axes")
    @Operation(summary = "Get own service axes")
    public ResponseEntity<List<ServiceAxisSelectionResponse>> getMyServiceAxes(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.getMyServiceAxes(currentUser));
    }

    // ── Enabled tools ─────────────────────────────────────────────────────────

    @PutMapping("/me/enabled-tools")
    @Operation(summary = "Enable or disable specific tools — disabling an axis tool returns a warning flag")
    public ResponseEntity<List<EnabledToolResponse>> updateEnabledTools(
            @Valid @RequestBody UpdateEnabledToolsRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.updateEnabledTools(request, currentUser));
    }

    @GetMapping("/me/enabled-tools")
    @Operation(summary = "Get all enabled/disabled tools for own specialist profile")
    public ResponseEntity<List<EnabledToolResponse>> getMyEnabledTools(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(specialistService.getMyEnabledTools(currentUser));
    }
}
