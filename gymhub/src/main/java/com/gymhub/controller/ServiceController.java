package com.gymhub.controller;

import com.gymhub.domain.gymservice.ServiceStatus;
import com.gymhub.dto.request.CreateServiceRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.ServiceResponse;
import com.gymhub.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/services")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Manage gym services and classes (free-name, no fixed taxonomy)")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @PostMapping
    @Operation(summary = "Create a new service or class for this gym")
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable Long gymId,
            @Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCatalogService.createService(gymId, request));
    }

    @GetMapping
    @Operation(summary = "List all services for this gym")
    public ResponseEntity<PagedResponse<ServiceResponse>> getServices(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(serviceCatalogService.getServices(gymId, pageable)));
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "Get a specific service")
    public ResponseEntity<ServiceResponse> getService(
            @PathVariable Long gymId,
            @PathVariable Long serviceId) {
        return ResponseEntity.ok(serviceCatalogService.getService(gymId, serviceId));
    }

    @PutMapping("/{serviceId}")
    @Operation(summary = "Update a service's details")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long gymId,
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateServiceRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(gymId, serviceId, request));
    }

    @PatchMapping("/{serviceId}/status")
    @Operation(summary = "Activate or deactivate a service")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long gymId,
            @PathVariable Long serviceId,
            @RequestParam ServiceStatus status) {
        serviceCatalogService.toggleStatus(gymId, serviceId, status);
        return ResponseEntity.noContent().build();
    }
}
