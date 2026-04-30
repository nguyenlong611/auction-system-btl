package com.adjuge.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction {

    /** Unique identifier for this bid transaction. */
    private final String id;

    /** The ID of the auction this bid was placed on. */
    private final String auctionId;

    /** The ID of the user who placed this bid. */
    private final String bidderId;

    /** The display name of the bidder (stored for convenience). */
    private final String bidderName;

    /** The dollar amount of this bid. */
    private final double amount;

    /** The exact date and time the bid was placed. */
    private final LocalDateTime timestamp;

    public BidTransaction(String id, String auctionId, String bidderId,
                          String bidderName, double amount, LocalDateTime timestamp) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // ── Getters (all fields are read-only) ───────────────────────────────

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a formatted string summarizing this bid.
     * Example: "Bid #BID-001 | $250.00 by Alice Smith on AUC-042 at 2026-03-28 14:30"
     */
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return String.format("Bid #%s | $%.2f by %s on %s at %s",
                id, amount, bidderName, auctionId, timestamp.format(formatter));
    }
}
