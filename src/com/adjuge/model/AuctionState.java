package com.adjuge.model;

/**
 * Enum representing the lifecycle states of an Auction.
 *
 * An auction moves through these states in order:
 *   OPEN -> RUNNING -> FINISHED -> PAID
 * At any point before PAID, it may also be CANCELED.
 *
 *   OPEN      — the auction has been created but bidding has not started yet.
 *   RUNNING   — bidding is currently active; users can place bids.
 *   FINISHED  — the end time has passed; no more bids are accepted.
 *   PAID      — the winning bidder has completed payment.
 *   CANCELED  — the auction was canceled by the seller or an admin.
 */
public enum AuctionState {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
