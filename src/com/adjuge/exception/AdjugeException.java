package com.adjuge.exception;

/**
 * Base exception for the BidVault auction system.
 *
 * All custom exceptions in the application extend this class.
 * This lets callers catch a single type (AdjugeException) to handle
 * any application-specific error, or catch a specific subclass
 * (e.g., InvalidBidException) for finer-grained handling.
 */
public class AdjugeException extends Exception {

    /**
     * Create an exception with just a message.
     *
     * @param message human-readable description of what went wrong
     */
    public AdjugeException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and an underlying cause.
     * Use this when wrapping a lower-level exception.
     *
     * @param message human-readable description
     * @param cause   the original exception that caused this one
     */
    public AdjugeException(String message, Throwable cause) {
        super(message, cause);
    }
}
