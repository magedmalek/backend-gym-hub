package com.gymhub.controller;

import com.gymhub.dto.request.SellExtraServiceRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.ExtraServiceSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/extra-services")
@RequiredArgsConstructor
@Tag(name = "Extra Services", description = "Sell paid add-on services outside the subscription (independent transactions)")
public class ExtraServiceController {

    private final ExtraServiceSaleService extraServiceSaleService;

    @PostMapping("/sell")
    @Operation(summary = "Sell a paid extra service to a customer (creates an independent financial transaction)")
    public ResponseEntity<?> sellExtraService(
            @PathVariable Long gymId,
            @Valid @RequestBody SellExtraServiceRequest request,
            @RequestParam Long employeeId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extraServiceSaleService.sellExtraService(gymId, request, employeeId));
    }

    @PostMapping("/usage")
    @Operation(summary = "Record use of a bundled (included) service from a subscription (no charge)")
    public ResponseEntity<?> recordIncludedUsage(
            @PathVariable Long gymId,
            @RequestParam Long customerId,
            @RequestParam Long serviceId,
            @RequestParam Long subscriptionId,
            @RequestParam Long employeeId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(extraServiceSaleService.recordIncludedServiceUsage(
                        gymId, customerId, serviceId, subscriptionId, employeeId, notes));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all extra service transactions for this gym")
    public ResponseEntity<PagedResponse<?>> getGymTransactions(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(extraServiceSaleService.getGymExtraTransactions(gymId, pageable)));
    }

    @GetMapping("/transactions/customer/{customerId}")
    @Operation(summary = "List extra service transactions for a specific customer")
    public ResponseEntity<PagedResponse<?>> getCustomerTransactions(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(extraServiceSaleService.getCustomerExtraTransactions(customerId, pageable)));
    }

    @GetMapping("/usages/customer/{customerId}")
    @Operation(summary = "List bundled service usages for a specific customer")
    public ResponseEntity<PagedResponse<?>> getCustomerUsages(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(extraServiceSaleService.getCustomerServiceUsages(customerId, pageable)));
    }
}
