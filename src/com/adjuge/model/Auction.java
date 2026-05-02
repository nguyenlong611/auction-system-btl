package com.adjuge.model;

import com.adjuge.exception.AuctionClosedException;
import com.adjuge.exception.InvalidBidException;
import com.adjuge.pattern.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private LocalDateTime endTime;

    /** Ordered list of all bids placed on this auction. */
    private final List<BidTransaction> bids;

    /** The current highest bid amount (starts at the item's start price). */
    private double currentHighestBid;

    /** The user ID of the current highest bidder (null if no bids yet). */
    private String highestBidderId;

    private final BidValidationStrategy validator;

    /**
     * List of observers watching this auction.
     * Uses CopyOnWriteArrayList for thread-safe iteration during notifications.
     */
    private final List<AuctionObserver> observers;

    public Auction(String id, Item item, String sellerId, String sellerName, double startPrice, LocalDateTime startTime, LocalDateTime endTime, ValidatorType validatorType ) {
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
        this.validator = BidValidatorFactory.getValidator(validatorType);
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

    public BidValidationStrategy getValidator() {
        return validator;
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
        setState(AuctionState.RUNNING);
        System.out.println("Auction " + this.id + " has started!");
    }

    // 2. Chuyển từ RUNNING -> FINISHED
    public synchronized void finish() {
        if (this.state != AuctionState.RUNNING) {
            throw new IllegalStateException("Can only finish an auction that is in the RUNNING state.");
        }
        setState(AuctionState.FINISHED);
        System.out.println("Auction " + this.id + " has finished, waiting for payment.");
    }

    // 3. Chuyển từ FINISHED -> PAID
    public synchronized void markAsPaid() {
        if (this.state != AuctionState.FINISHED) {
            throw new IllegalStateException("Must wait for the auction to finish before marking as paid.");
        }
        setState(AuctionState.PAID);
        System.out.println("Auction " + this.id + " has been paid.");
    }

    // 4. Chuyển sang CANCELED (Có thể xảy ra từ nhiều trạng thái)
    public synchronized void cancel() {
        if (this.state == AuctionState.PAID) {
            throw new IllegalStateException("Cannot cancel a paid auction.");
        }
        setState(AuctionState.CANCELED);
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

    // ── Time utilities ───────────────────────────────────────────────────

    /**
     * Checks whether the auction's end time has passed.
     *
     * @return true if the current time is after endTime
     */
    public boolean isStarted() {return !(this.state == AuctionState.OPEN);}

    public boolean isExpired() {
        return endTime.isBefore(LocalDateTime.now());
    }

    public void applyAntiSniping() {
        LocalDateTime currentTime = LocalDateTime.now();
        long secondsRemaining = ChronoUnit.SECONDS.between(currentTime, this.endTime);
        if  (secondsRemaining < 10) {
            this.endTime = this.endTime.plusSeconds(60);
        }
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
