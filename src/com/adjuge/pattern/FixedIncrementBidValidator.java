package com.adjuge.pattern;

import com.adjuge.exception.InvalidBidException;
import com.adjuge.model.Auction;
import com.adjuge.model.AuctionState;
import com.adjuge.model.User;

public class FixedIncrementBidValidator implements BidValidationStrategy {
    private final double buocGia; // Ví dụ: Bước giá là 50.000 VNĐ
    public FixedIncrementBidValidator(double buocGia) {
        this.buocGia = buocGia;
    }

    @Override
    public void validate(Auction auction, double amount) throws InvalidBidException {

        double giaToiThieu = auction.getCurrentHighestBid() + buocGia;
        if (amount < giaToiThieu) {
            throw new InvalidBidException("This bid must be " + buocGia + " VNĐ higher than the last bid!");
        }
    }
}
