package com.adjuge.pattern;

import com.adjuge.exception.AuctionClosedException;
import com.adjuge.model.Auction;
import com.adjuge.model.User;
import com.adjuge.exception.InvalidBidException;

/**
 * STRATEGY PATTERN — Defines a bid validation algorithm.
 *
 * Different implementations of this interface can enforce different
 * bidding rules. For example:
 *   - StandardBidValidation: checks minimum increment, auction open, etc.
 *   - PremiumBidValidation: allows smaller increments for VIP users.
 *
 * The service layer holds a reference to a BidValidationStrategy and
 * can swap it out at runtime without changing any other code.
 */
public interface BidValidationStrategy {

    void validate(Auction auction, double amount) throws InvalidBidException, AuctionClosedException;
}
