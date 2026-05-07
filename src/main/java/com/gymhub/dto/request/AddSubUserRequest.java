package com.gymhub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddSubUserRequest {

    /** The customerId of the sub-user (must be an existing Customer in this gym). */
    @NotNull(message = "Sub-user customer ID is required")
    private Long subCustomerId;

    /** Optional relation label, e.g. "Spouse", "Child". */
    @Size(max = 100)
    private String relationType;
}
