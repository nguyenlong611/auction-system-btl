package com.adjuge.pattern;

import com.adjuge.exception.InvalidBidException;
import com.adjuge.model.Auction;
import com.adjuge.model.User;

public class VipJumpBidValidator implements BidValidationStrategy {
    @Override
    public void validate(Auction auction, double amount) throws InvalidBidException {
        // VIP room requires at least a 20% jump from the current highest bid
        double minimumRequiredBid = auction.getCurrentHighestBid() * 1.20;

        if (amount < minimumRequiredBid) {
            throw new InvalidBidException("VIP Room strictly requires a bid of at least 20% higher than the current bid (" + minimumRequiredBid + ")!");
        }
    }
}
