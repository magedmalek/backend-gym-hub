package com.gymhub.domain.subscription;

/**
 * Payment methods accepted by the platform.
 * CASH is the only method with a real implementation.
 * Remaining values are structural — no gateway is integrated.
 */
public enum PaymentMethod {
    CASH,
    ONLINE_PAYMENT_READY,
    EXTERNAL_PAYMENT,
    PAYMENT_AT_GYM,
    PAYMENT_TO_SPECIALIST,
    WALLET_FUTURE
}
