package com.adjuge.exception;

/**
 * Thrown when authentication or registration fails.
 *
 * Common scenarios:
 *   - Email/password combination is incorrect during login
 *   - Email is already registered during sign-up
 *   - User account is not found
 */
public class AuthenticationException extends AdjugeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
