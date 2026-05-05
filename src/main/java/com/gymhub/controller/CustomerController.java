package com.gymhub.controller;

import com.gymhub.domain.user.User;
import com.gymhub.dto.request.CreateCustomerRequest;
import com.gymhub.dto.response.CustomerResponse;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.service.CustomerService;
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
@RequestMapping("/api/v1/gyms/{gymId}/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Register and manage gym members, search, QR / barcode lookup")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Register a new customer — requires MANAGE_CUSTOMERS permission")
    public ResponseEntity<CustomerResponse> createCustomer(
            @PathVariable Long gymId,
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(gymId, request, currentUser));
    }

    @GetMapping
    @Operation(summary = "List all customers of this gym")
    public ResponseEntity<PagedResponse<CustomerResponse>> getCustomers(
            @PathVariable Long gymId,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(customerService.getCustomers(gymId, currentUser, pageable)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by name, email or member code")
    public ResponseEntity<PagedResponse<CustomerResponse>> searchCustomers(
            @PathVariable Long gymId,
            @RequestParam String q,
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(
                PagedResponse.from(customerService.searchCustomers(gymId, q, currentUser, pageable)));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a specific customer's profile")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerService.getCustomer(gymId, customerId, currentUser));
    }

    @GetMapping("/by-code/{memberCode}")
    @Operation(summary = "Look up a customer by barcode / QR code value (used at entry)")
    public ResponseEntity<CustomerResponse> getByMemberCode(
            @PathVariable Long gymId,
            @PathVariable String memberCode,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(customerService.getByMemberCode(gymId, memberCode, currentUser));
    }

    @PatchMapping("/{customerId}/status")
    @Operation(summary = "Activate or deactivate a customer — requires MANAGE_CUSTOMERS permission")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            @RequestParam boolean active,
            @AuthenticationPrincipal User currentUser) {
        customerService.toggleStatus(gymId, customerId, active, currentUser);
        return ResponseEntity.noContent().build();
    }
}
