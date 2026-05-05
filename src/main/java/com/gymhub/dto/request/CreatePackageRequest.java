package com.gymhub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class CreatePackageRequest {

    @NotBlank(message = "Package name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @Size(max = 10)
    private String currency = "EGP";

    @Min(0)
    private int maxInvitations = 0;

    private boolean allowGuestRepeatVisit = false;

    private boolean allowPartialPayment = false;

    /** IDs of GymService entities to include in this package. */
    private Set<Long> includedServiceIds;
}
