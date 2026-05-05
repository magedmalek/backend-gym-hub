package com.gymhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCustomerRequest {

    // ── Option A: link existing platform user ────────────────────────────────
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

    @Size(max = 20)
    private String phone;

    @Size(min = 6)
    private String password;

    // ── Member code (optional — can be auto-generated) ───────────────────────
    @Size(max = 100)
    private String memberCode;
}
