package com.gymhub.exception;

/**
 * Thrown when the dashboard tries to create a customer whose phone is already
 * registered on the platform. The existing user must use the customer app to
 * request linking instead of being silently duplicated.
 */
public class UserAlreadyExistsException extends RuntimeException {

    private final String phone;

    public UserAlreadyExistsException(String phone) {
        super("A user with phone " + phone + " is already registered on the platform");
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
}
