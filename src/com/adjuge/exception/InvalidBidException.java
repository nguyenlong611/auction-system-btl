package com.adjuge.exception;

/**
 * Thrown when a bid is not valid.
 *
 * Common reasons:
 *   - Bid amount is lower than the current highest bid
 *   - Bid amount does not meet the minimum increment
 *   - The seller is trying to bid on their own auction
 *   - The bidder already holds the highest bid
 */
public class InvalidBidException extends AdjugeException {

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
