package com.gymhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UseInvitationRequest {

    @NotNull(message = "Host customer ID is required")
    private Long hostCustomerId;

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    // ── Guest information ────────────────────────────────────────────────────

    /** If the guest already exists as a platform user, supply their ID. */
    private Long existingGuestUserId;

    /** If the guest is new, supply their details to create a minimal account. */
    @Size(max = 100)
    private String guestFirstName;

    @Size(max = 100)
    private String guestLastName;

    @Email
    @Size(max = 150)
    private String guestEmail;

    @Size(max = 20)
    private String guestPhone;
}
