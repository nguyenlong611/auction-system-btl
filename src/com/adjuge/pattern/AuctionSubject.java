package com.adjuge.pattern;

/**
 * OBSERVER PATTERN — Subject interface.
 *
 * Any class that can be "watched" for changes implements this interface.
 * Observers register themselves here, and the subject notifies them
 * when something interesting happens (like a new bid).
 */
public interface AuctionSubject {

    /**
     * Register an observer to receive auction event notifications.
     *
     * @param observer the observer to add
     */
    void addObserver(AuctionObserver observer);

    /**
     * Remove an observer so it no longer receives notifications.
     *
     * @param observer the observer to remove
     */
    void removeObserver(AuctionObserver observer);

    /**
     * Notify all observers that a new bid has been placed.
     *
     * @param tx the new bid transaction
     */
    void notifyNewBid(com.adjuge.model.BidTransaction tx);

    /**
     * Notify all observers that the auction state has changed.
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    void notifyStateChange(com.adjuge.model.AuctionState oldState, com.adjuge.model.AuctionState newState);
}
