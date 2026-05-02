package com.gymhub.domain.gym;

/**
 * Determines when a paid subscription becomes active.
 */
public enum ActivationPolicy {

    /** Subscription activates automatically the moment all payment conditions are met. */
    IMMEDIATE,

    /** Subscription must be manually activated by an authorised employee or the gym admin. */
    MANUAL
}
