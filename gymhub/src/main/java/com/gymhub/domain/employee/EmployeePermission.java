package com.gymhub.domain.employee;

/**
 * Fine-grained operational permissions assignable to an employee.
 * The gym admin decides which combination each employee receives.
 */
public enum EmployeePermission {

    /** Can create and process subscription sales. */
    SELL_SUBSCRIPTION,

    /** Can activate / delay a subscription that has been sold. */
    ACTIVATE_SUBSCRIPTION,

    /** Can record member attendance (barcode / QR scan). */
    REGISTER_ATTENDANCE,

    /** Can register guest invitations on behalf of a member. */
    REGISTER_INVITATION,

    /** Can sell extra (paid) services outside a subscription. */
    SELL_EXTRA_SERVICE,

    /** Can manage gym services (create / edit / deactivate). */
    MANAGE_SERVICES,

    /** Can manage packages (create / edit / deactivate). */
    MANAGE_PACKAGES,

    /** Can view and manage customer profiles. */
    MANAGE_CUSTOMERS,

    /** Full admin access — typically the gym owner only. */
    ADMIN
}
