package com.gymhub.dto.request;

import com.gymhub.domain.subscription.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddPaymentRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be positive")
    private BigDecimal amount;

    /** Phase 1: only CASH is accepted. Any other value is rejected at the service layer. */
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    private String notes;
}
