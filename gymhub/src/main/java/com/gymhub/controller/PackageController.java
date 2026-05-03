package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.CreatePackageRequest;
import com.gymhub.dto.response.PackageResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.PackageService;
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
@RequestMapping("/api/v1/gyms/{gymId}/packages")
@RequiredArgsConstructor
@Tag(name = "Subscription Packages", description = "Define subscription packages offered by the gym")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Create a new subscription package — requires MANAGE_PACKAGES permission")
    public ResponseEntity<PackageResponse> createPackage(
            @PathVariable Long gymId,
            @Valid @RequestBody CreatePackageRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.createPackage(gymId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List all packages for this gym")
    public ResponseEntity<PagedResponse<PackageResponse>> getPackages(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(packageService.getPackages(gymId, currentUser, pageable)));
    }

    @GetMapping("/{packageId}")
    @Operation(summary = "Get a specific package")
    public ResponseEntity<PackageResponse> getPackage(
            @PathVariable Long gymId,
            @PathVariable Long packageId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(packageService.getPackage(gymId, packageId, currentUser));
    }

    @PutMapping("/{packageId}")
    @Operation(summary = "Update a package's details — requires MANAGE_PACKAGES permission")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long gymId,
            @PathVariable Long packageId,
            @Valid @RequestBody CreatePackageRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                packageService.updatePackage(gymId, packageId, request, currentUser));
    }

    @PatchMapping("/{packageId}/status")
    @Operation(summary = "Activate or deactivate a package — requires MANAGE_PACKAGES permission")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long gymId,
            @PathVariable Long packageId,
            @RequestParam boolean active,
            @AuthenticationPrincipal User currentUser) {
        packageService.toggleStatus(gymId, packageId, active, currentUser);
        return ResponseEntity.noContent().build();
    }
}
