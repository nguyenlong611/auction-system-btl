package com.adjuge.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.adjuge.model.*;
import com.adjuge.pattern.BidValidatorFactory;
import com.adjuge.pattern.DataStore;
import com.adjuge.exception.AuctionClosedException;
import com.adjuge.exception.InvalidBidException;
import com.adjuge.pattern.BidValidationStrategy;
import com.adjuge.pattern.StandardBidValidator;
import com.adjuge.util.TimeUtil;

/**
 * Handles bid placement with thread safety and validation.
 *
 * WHY SYNCHRONIZED?
 * In a real auction system, two users might try to bid at the exact same moment.
 * Without synchronization, a "race condition" could occur:
 *   - User A reads the current highest bid as $100
 *   - User B reads the current highest bid as $100 (before A's bid is saved)
 *   - Both submit $110, and one bid overwrites the other (a "lost update")
 *
 * By using synchronized on the auction object, we ensure only one thread can
 * process a bid on a given auction at a time. Different auctions can still be
 * bid on in parallel since each auction is a separate lock.
 *
 * WHY STRATEGY PATTERN?
 * The bid validation rules (minimum bid, who can bid, etc.) are encapsulated
 * in a BidValidationStrategy. This means we can easily swap in different rules
 * (e.g., VIP auctions with higher minimums) without changing this class.
 */
public class BidService {

    // The validation strategy determines the rules for accepting/rejecting bids.
    // Using the Strategy pattern so validation rules can be swapped easily.

    /**
     * Places a bid on an auction.
     *
     * This method is SYNCHRONIZED on the auction object to prevent race conditions.
     * Two bidders cannot place bids on the same auction at the same time — one will
     * wait until the other finishes. However, bids on *different* auctions can
     * proceed in parallel since each auction is a separate lock.
     *
     * @param auction the auction to bid on
     * @param bidder  the user placing the bid
     * @param amount  the bid amount in dollars
     * @return the created BidTransaction record
     * @throws InvalidBidException    if the bid fails validation (too low, own auction, etc.)
     * @throws AuctionClosedException if the auction is not accepting bids
     */
    public BidTransaction placeBid(Auction auction, User bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        // Synchronize on the specific auction object so different auctions
        // can be bid on in parallel, but the SAME auction is locked
        synchronized (auction) {

            if (!(auction.isStarted())) {
                throw new AuctionClosedException("Phiên đấu gía chưa bắt đầu");
            }

            // --- Check 2: Has the auction's end time passed? ---
            if (auction.isExpired()) {
                // Transition the auction to FINISHED since it has expired
                auction.finish();
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

            // --- Check 3: Validate the bid using the Strategy pattern ---
            // This delegates to StandardBidValidator (or any other strategy)
            auction.getValidator().validate(auction, amount);

            // --- All checks passed — create the bid transaction ---

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

            // Persist to database
            DataStore.getInstance().saveBid(tx, auction);
            auction.applyAntiSniping();
            return tx;
        }
    }
}
