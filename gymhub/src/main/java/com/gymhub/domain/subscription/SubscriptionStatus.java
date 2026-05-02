package com.gymhub.domain.subscription;

public enum SubscriptionStatus {

    /** Created but not yet fully paid. */
    PENDING_PAYMENT,

    /** Fully paid but activation is pending (manual policy or delayed start). */
    PENDING_ACTIVATION,

    /** Active and within its validity window. */
    ACTIVE,

    /** Validity window has passed. */
    EXPIRED,

    /** Cancelled before or during its active period. */
    CANCELLED
}
