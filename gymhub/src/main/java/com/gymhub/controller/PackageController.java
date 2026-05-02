package com.gymhub.controller;

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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/packages")
@RequiredArgsConstructor
@Tag(name = "Subscription Packages", description = "Define subscription packages offered by the gym")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Create a new subscription package")
    public ResponseEntity<PackageResponse> createPackage(
            @PathVariable Long gymId,
            @Valid @RequestBody CreatePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.createPackage(gymId, request));
    }

    @GetMapping
    @Operation(summary = "List all packages for this gym")
    public ResponseEntity<PagedResponse<PackageResponse>> getPackages(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(packageService.getPackages(gymId, pageable)));
    }

    @GetMapping("/{packageId}")
    @Operation(summary = "Get a specific package")
    public ResponseEntity<PackageResponse> getPackage(
            @PathVariable Long gymId,
            @PathVariable Long packageId) {
        return ResponseEntity.ok(packageService.getPackage(gymId, packageId));
    }

    @PutMapping("/{packageId}")
    @Operation(summary = "Update a package's details")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long gymId,
            @PathVariable Long packageId,
            @Valid @RequestBody CreatePackageRequest request) {
        return ResponseEntity.ok(packageService.updatePackage(gymId, packageId, request));
    }

    @PatchMapping("/{packageId}/status")
    @Operation(summary = "Activate or deactivate a package")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long gymId,
            @PathVariable Long packageId,
            @RequestParam boolean active) {
        packageService.toggleStatus(gymId, packageId, active);
        return ResponseEntity.noContent().build();
    }
}
