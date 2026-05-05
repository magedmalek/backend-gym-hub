package com.gymhub.domain.gym;

/**
 * Supported member-entry methods for a gym.
 * Each gym can enable one or more of these.
 * (Fingerprint is excluded from the current scope.)
 */
public enum EntranceMethod {

    /** Physical barcode card / tag scanned by a dedicated device. */
    BARCODE,

    /** One-time dynamic QR code generated and shown by the receptionist's screen. */
    DYNAMIC_QR,

    /** Static printed QR code carried by the member. */
    PRINTED_QR
}
