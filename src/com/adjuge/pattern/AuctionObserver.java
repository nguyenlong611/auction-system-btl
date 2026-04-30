package com.adjuge.pattern;

import com.adjuge.model.Auction;
import com.adjuge.model.AuctionState;
import com.adjuge.model.BidTransaction;

/**
 * OBSERVER PATTERN — Observer interface.
 *
 * Any class that wants to be notified about auction events
 * (new bids, state changes) must implement this interface.
 * The subject (Auction) calls these methods whenever something happens.
 */
public interface AuctionObserver {

    /**
     * Called when a new bid is placed on the auction.
     *
     * @param auction the auction that received the bid
     * @param bid     the new bid transaction
     */
    void onNewBid(Auction auction, BidTransaction bid);

    /**
     * Called when the auction's state changes
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    void onStateChange(Auction auction, AuctionState oldState, AuctionState newState);
}
