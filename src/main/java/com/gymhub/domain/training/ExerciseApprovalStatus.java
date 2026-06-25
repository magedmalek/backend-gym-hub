package com.gymhub.domain.training;

/**
 * Moderation status of a library exercise (Phase 2 plan — Section 11).
 * Global/platform exercises are APPROVED; provider-created ones start PENDING_APPROVAL.
 */
public enum ExerciseApprovalStatus {
    APPROVED,
    PENDING_APPROVAL,
    REJECTED,
    ARCHIVED
}
