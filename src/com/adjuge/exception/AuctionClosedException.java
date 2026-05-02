package com.adjuge.exception;

/**
 * Thrown when someone tries to interact with a closed or ended auction.
 *
 * Common scenarios:
 *   - Placing a bid after the auction end time has passed
 *   - Placing a bid on an auction whose state is ENDED or CANCELLED
 */
public class AuctionClosedException extends AdjugeException {

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
