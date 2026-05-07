package com.gymhub.domain.subscription;

/** Indicates how a subscription was transitioned to ACTIVE status. */
public enum ActivationType {
    /** Activated automatically by the system (e.g. full payment received + IMMEDIATE policy). */
    AUTOMATIC,
    /** Activated manually by an employee or admin from the dashboard. */
    MANUAL
}
