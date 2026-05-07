package com.gymhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    // ── Phone (primary lookup key for new customers) ─────────────────────────
    /**
     * Phone number used to check whether this person already exists on the platform.
     * If a user with this phone already exists, the dashboard cannot create a duplicate —
     * the existing user must link via the customer app.
     * Required when not using existingUserId.
     */
    @Size(max = 20)
    private String phone;

    // ── Option A: link existing platform user (by ID) ───────────────────────
    /** If provided, the customer record is linked to an existing user. */
    private Long existingUserId;

    // ── Option B: create a new user + customer in one shot ──────────────────
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Email
    @Size(max = 150)
    private String email;

    @Size(min = 6)
    private String password;

    // ── Member code (optional — can be auto-generated) ───────────────────────
    @Size(max = 100)
    private String memberCode;
}
