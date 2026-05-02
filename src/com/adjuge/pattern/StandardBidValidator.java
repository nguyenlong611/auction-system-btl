package com.adjuge.pattern;

import com.adjuge.exception.InvalidBidException;
import com.adjuge.model.Auction;
import com.adjuge.util.TimeUtil;

public class StandardBidValidator implements BidValidationStrategy {

    @Override
    public void validate(Auction auction, double amount) throws InvalidBidException {

        if (amount <= auction.getCurrentHighestBid()) {
            throw new InvalidBidException(
                    "Bid must be higher than current bid: " +
                            TimeUtil.formatCurrency(auction.getCurrentHighestBid())
            );
        }
    }
}
