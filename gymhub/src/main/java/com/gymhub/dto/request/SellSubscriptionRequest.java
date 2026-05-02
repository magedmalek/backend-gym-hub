package com.gymhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SellSubscriptionRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotNull(message = "Initial payment amount is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal initialPaymentAmount;

    /**
     * Optional: if provided, activation (and subscription start) is deferred to this date.
     * If null, activation follows the gym's ActivationPolicy.
     */
    private LocalDate deferredStartDate;
}
