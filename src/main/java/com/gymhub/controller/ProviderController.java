package com.gymhub.controller;

import com.gymhub.domain.provider.ProviderType;
import com.gymhub.domain.user.User;
import com.gymhub.dto.request.provider.UpdateProviderRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.provider.ProviderResponse;
import com.gymhub.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Provider profile management (gyms and specialists)")
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping("/me")
    @Operation(summary = "Get all provider profiles owned by the current user")
    public ResponseEntity<List<ProviderResponse>> getMyProviders(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(providerService.getMyProviders(currentUser));
    }

    @GetMapping("/{providerId}")
    @Operation(summary = "Get a provider profile by ID")
    public ResponseEntity<ProviderResponse> getProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(providerService.getProvider(providerId));
    }

    @GetMapping
    @Operation(summary = "List providers — filter by type (GYM or SPECIALIST)")
    public ResponseEntity<PagedResponse<ProviderResponse>> listProviders(
            @RequestParam(required = false) ProviderType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                PagedResponse.from(providerService.listProviders(type, PageRequest.of(page, size))));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own provider profile — requires providerId query param when owning multiple")
    public ResponseEntity<ProviderResponse> updateProvider(
            @RequestParam Long providerId,
            @Valid @RequestBody UpdateProviderRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(providerService.updateProvider(providerId, request, currentUser));
    }
}
