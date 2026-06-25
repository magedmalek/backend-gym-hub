package com.gymhub.domain.notification;

/**
 * Delivery channel for a notification. IN_APP is always persisted;
 * PUSH/SMS/EMAIL are external-delivery markers (gateways configured via app.infobip / spring.mail).
 */
public enum NotificationChannel {
    IN_APP,
    PUSH,
    SMS,
    EMAIL
}
