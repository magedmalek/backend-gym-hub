package com.gymhub.domain.request;

/**
 * Workflow status of a complaint (Phase 2 plan — Section 28).
 */
public enum ComplaintStatus {
    NEW,
    UNDER_REVIEW,
    NEEDS_RESPONSE,
    RESOLVED,
    REJECTED,
    CLOSED
}
