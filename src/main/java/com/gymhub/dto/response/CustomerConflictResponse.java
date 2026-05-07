package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Returned with HTTP 409 when the dashboard tries to create a customer
 * whose phone number is already registered on the platform.
 *
 * The frontend must instruct the existing user to request linking via the
 * customer mobile app instead of creating a duplicate account.
 */
@Data
@Builder
public class CustomerConflictResponse {

    /** Machine-readable status code. */
    private String status;

    /** Human-readable explanation shown to the dashboard operator. */
    private String message;
}
