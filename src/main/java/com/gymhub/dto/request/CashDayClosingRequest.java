package com.gymhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request to close the cash day for a gym. POST /api/v1/gyms/{gymId}/cash-closings.
 */
@Data
public class CashDayClosingRequest {

    /** Business date to close; defaults to today when omitted. */
    @PastOrPresent(message = "Business date cannot be in the future")
    private LocalDate businessDate;

    @NotNull(message = "Counted cash total is required")
    @DecimalMin(value = "0.0", message = "Counted cash total cannot be negative")
    private BigDecimal countedCashTotal;

    @Size(max = 1000)
    private String notes;
}
