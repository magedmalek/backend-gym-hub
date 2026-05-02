package com.gymhub.controller;

import com.gymhub.dto.request.AddPaymentRequest;
import com.gymhub.dto.request.SellSubscriptionRequest;
import com.gymhub.dto.response.PagedResponse;
import com.gymhub.dto.response.SubscriptionResponse;
import com.gymhub.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/gyms/{gymId}/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Sell, pay, activate and manage member subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/sell")
    @Operation(summary = "Sell a subscription to a customer (creates subscription + records initial payment)")
    public ResponseEntity<SubscriptionResponse> sellSubscription(
            @PathVariable Long gymId,
            @Valid @RequestBody SellSubscriptionRequest request,
            @RequestParam Long sellerEmployeeId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.sellSubscription(gymId, request, sellerEmployeeId));
    }

    @PostMapping("/{subscriptionId}/payments")
    @Operation(summary = "Add a payment instalment to a subscription")
    public ResponseEntity<SubscriptionResponse> addPayment(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @Valid @RequestBody AddPaymentRequest request,
            @RequestParam Long employeeId) {
        return ResponseEntity.ok(
                subscriptionService.addPayment(gymId, subscriptionId, request, employeeId));
    }

    @PostMapping("/{subscriptionId}/activate")
    @Operation(summary = "Manually activate a fully-paid subscription (sets start/end dates)")
    public ResponseEntity<SubscriptionResponse> activateSubscription(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId,
            @RequestParam Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        return ResponseEntity.ok(
                subscriptionService.activateSubscription(gymId, subscriptionId, employeeId, startDate));
    }

    @GetMapping
    @Operation(summary = "List all subscriptions for this gym")
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getByGym(
            @PathVariable Long gymId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(subscriptionService.getByGym(gymId, pageable)));
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get a specific subscription")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable Long gymId,
            @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(subscriptionService.getSubscription(gymId, subscriptionId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all subscriptions for a specific customer")
    public ResponseEntity<PagedResponse<SubscriptionResponse>> getByCustomer(
            @PathVariable Long gymId,
            @PathVariable Long customerId,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(subscriptionService.getByCustomer(customerId, pageable)));
    }
}
