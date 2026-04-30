package com.adjuge.model;

import com.adjuge.exception.AuctionClosedException;
import com.adjuge.exception.InvalidBidException;
import com.adjuge.pattern.AuctionObserver;
import com.adjuge.pattern.AuctionSubject;
import com.adjuge.pattern.StandardBidValidator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements AuctionSubject {

    /** Unique identifier for this auction (e.g. "AUC-001"). */
    private final String id;

    /**
     * The item being auctioned.
     * Declared as Item (abstract type) — POLYMORPHISM in action.
     */
    private final Item item;

    /** The ID of the seller who created this auction. */
    private final String sellerId;

    /** The display name of the seller. */
    private final String sellerName;

    /** The current lifecycle state of this auction. */
    private AuctionState state;

    /** When bidding starts. */
    private final LocalDateTime startTime;

    /** When bidding ends. */
    private final LocalDateTime endTime;

    /** Ordered list of all bids placed on this auction. */
    private final List<BidTransaction> bids;

    /** The current highest bid amount (starts at the item's start price). */
    private double currentHighestBid;

    /** The user ID of the current highest bidder (null if no bids yet). */
    private String highestBidderId;

    private StandardBidValidator validator;

    /**
     * List of observers watching this auction.
     * Uses CopyOnWriteArrayList for thread-safe iteration during notifications.
     */
    private final List<AuctionObserver> observers;

    public Auction(String id, Item item, String sellerId, String sellerName,
                   double startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.state = AuctionState.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentHighestBid = startPrice;
        this.highestBidderId = null;
        this.bids = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    /** Returns the item — could be Electronics, Art, Vehicle, etc. (POLYMORPHISM). */
    public Item getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public AuctionState getState() {
        return state;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<BidTransaction> getBids() {
        return bids;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    // ── State management ─────────────────────────────────────────────────

    private void setState(AuctionState newState) {
        AuctionState oldState = this.state;
        this.state = newState;
        notifyStateChange(oldState, newState);
    }
    // 1. Chuyển từ OPEN -> RUNNING
    public synchronized void start() {
        if (this.state != AuctionState.OPEN) {
            throw new IllegalStateException("Can only start an auction that is in the OPEN state.");
        }
        this.state = AuctionState.RUNNING;
        System.out.println("Auction " + this.id + " has started!");
    }

    // 2. Chuyển từ RUNNING -> FINISHED
    public synchronized void finish() {
        if (this.state != AuctionState.RUNNING) {
            throw new IllegalStateException("Can only finish an auction that is in the RUNNING state.");
        }
        this.state = AuctionState.FINISHED;
        System.out.println("Auction " + this.id + " has finished, waiting for payment.");
    }

    // 3. Chuyển từ FINISHED -> PAID
    public synchronized void markAsPaid() {
        if (this.state != AuctionState.FINISHED) {
            throw new IllegalStateException("Must wait for the auction to finish before marking as paid.");
        }
        this.state = AuctionState.PAID;
        System.out.println("Auction " + this.id + " has been paid.");
    }

    // 4. Chuyển sang CANCELED (Có thể xảy ra từ nhiều trạng thái)
    public synchronized void cancel() {
        if (this.state == AuctionState.PAID) {
            throw new IllegalStateException("Cannot cancel a paid auction.");
        }
        this.state = AuctionState.CANCELED;
        System.out.println("Auction " + this.id + " has been canceled.");
    }

    // ── Bidding ──────────────────────────────────────────────────────────

    /**
     * Records a new bid on this auction.
     * Updates the highest bid and bidder, then notifies all observers.
     *
     * @param tx the bid transaction to add
     */
    public void addBid(BidTransaction tx) {
        bids.add(tx);
        this.currentHighestBid = tx.getAmount();
        this.highestBidderId = tx.getBidderId();
        notifyNewBid(tx);
    }

    public synchronized BidTransaction placeBid(Auction auction, User bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        if (auction.getState() != AuctionState.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá chưa mở");
        }

        // --- Check 2: Has the auction's end time passed? ---
        if (auction.isExpired()) {
            // Transition the auction to FINISHED since it has expired
            auction.setState(AuctionState.FINISHED);
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc");
        }

        // Rule 1: You cannot bid on your own auction (conflict of interest)
        if (bidder.getId().equals(auction.getSellerId())) {
            throw new InvalidBidException("You cannot bid on your own auction.");
        }

        // Rule 2: Check if the user's account type allows bidding
        // This uses polymorphism — canBid() returns different results for
        // Bidder, Seller, and Admin subclasses of User
        if (!(bidder instanceof Biddable)) {
            throw new InvalidBidException(
                    "Your account type (" + bidder.getRole() + ") cannot place bids."
            );
        }

            validator.validate(auction, amount);

            // Generate a unique transaction ID like "tx_3fa85f64"
            String txId = "tx_" + UUID.randomUUID().toString().substring(0, 8);

            // Format the bidder's name for display (e.g., "John D.")
            String bidderName = bidder.getFirstName() + " " + bidder.getLastName().charAt(0) + ".";

            // Create the bid transaction record
            BidTransaction tx = new BidTransaction(
                    txId,
                    auction.getId(),
                    bidder.getId(),
                    bidderName,
                    amount,
                    LocalDateTime.now()
            );

            // Record the bid on the auction
            // Auction.addBid() updates the currentHighestBid and notifies observers
            auction.addBid(tx);

            return tx;
        }

    // ── Time utilities ───────────────────────────────────────────────────

    /**
     * Checks whether the auction's end time has passed.
     *
     * @return true if the current time is after endTime
     */
    public boolean isExpired() {
        return endTime.isBefore(LocalDateTime.now());
    }

    /**
     * Returns the time remaining until the auction ends.
     * If the auction has already ended, the duration will be negative.
     *
     * @return a Duration representing the time left
     */
    public Duration getTimeRemaining() {
        return Duration.between(LocalDateTime.now(), endTime);
    }

    /**
     * Returns the total number of bids placed on this auction.
     */
    public int getBidCount() {
        return bids.size();
    }

    // ── AuctionSubject implementation (OBSERVER PATTERN) ─────────────────

    /**
     * Registers an observer to be notified of auction events.
     */
    @Override
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer so it no longer receives notifications.
     */
    @Override
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies every registered observer that a new bid has been placed.
     *
     * @param tx the new bid transaction
     */
    public void notifyNewBid(BidTransaction tx) {
        for (AuctionObserver observer : observers) {
            observer.onNewBid(this, tx);
        }
    }

    /**
     * Notifies every registered observer that the auction state has changed.
     *
     * @param oldState the previous state
     * @param newState the new state
     */
    public void notifyStateChange(AuctionState oldState, AuctionState newState) {
        for (AuctionObserver observer : observers) {
            observer.onStateChange(this, oldState, newState);
        }
    }
}
