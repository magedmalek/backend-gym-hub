package com.gymhub.domain.user;

/**
 * Roles a single unified User account may hold inside the platform.
 * One user can have multiple roles simultaneously (e.g. a gym owner who is also a member).
 */
public enum UserRole {

    /** Owner / admin of a gym entity. */
    SERVICE_PROVIDER,

    /** Staff member employed by a gym. */
    EMPLOYEE,

    /** Member / customer of a gym. */
    CUSTOMER
}
