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

    /** Extra bonus days added to the subscription end date at activation. */
    @Min(0)
    private int bonusDays = 0;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @Size(max = 10)
    private String currency = "EGP";

    /** Freeze allowance in days. 0 = no freeze allowed. */
    @Min(0)
    private int freezeAllowanceDays = 0;

    @Min(0)
    private int maxInvitations = 0;

    private boolean allowGuestRepeatVisit = false;

    private boolean allowPartialPayment = false;

    /** Set to true to enable family/sub-user mode. */
    private boolean isFamilyPackage = false;

    /** Max sub-users when isFamilyPackage = true. Ignored otherwise. */
    @Min(0)
    private int maxSubUsers = 0;

    /** IDs of GymService entities to include in this package. */
    private Set<Long> includedServiceIds;
}
