package com.adjuge.service;

import com.adjuge.exception.AuctionClosedException;
import com.adjuge.exception.InvalidBidException;
import com.adjuge.model.*;
import com.adjuge.pattern.ItemFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

    private BidService bidService;
    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        bidService = new BidService();
        seller = new Seller("u_seller1", "Seller", "One", "seller@example.com", "pass");
        bidder = new Bidder("u_bidder1", "Bidder", "One", "bidder@example.com", "pass");
        
        Item item = ItemFactory.createItem(Category.ELECTRONICS, "Laptop", "A fast laptop", 1000.0, "", "New", "Dell", "XPS", "12");
        
        auction = new Auction("auc_1", item, seller.getId(), seller.getFirstName(), 1000.0, 
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }

    @Test
    void testPlaceBidSuccess() throws InvalidBidException, AuctionClosedException {
        BidTransaction tx = bidService.placeBid(auction, bidder, 1100.0);
        
        assertNotNull(tx);
        assertEquals(1100.0, tx.getAmount());
        assertEquals(bidder.getId(), tx.getBidderId());
        assertEquals(1100.0, auction.getCurrentHighestBid());
        assertEquals(1, auction.getBidCount());
    }

    @Test
    void testBidTooLow() {
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            bidService.placeBid(auction, bidder, 900.0);
        });
        assertTrue(exception.getMessage().contains("must be higher than current bid"));
    }

    @Test
    void testBidOnOwnAuction() {
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            bidService.placeBid(auction, seller, 1200.0);
        });
        assertEquals("You cannot bid on your own auction.", exception.getMessage());
    }

    @Test
    void testBidOnExpiredAuction() {
        Auction expiredAuction = new Auction("auc_2", auction.getItem(), seller.getId(), seller.getFirstName(), 1000.0,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        
        Exception exception = assertThrows(AuctionClosedException.class, () -> {
            bidService.placeBid(expiredAuction, bidder, 1500.0);
        });
        assertEquals("This auction has ended.", exception.getMessage());
        assertEquals(AuctionState.FINISHED, expiredAuction.getState());
    }

    @Test
    void testBidExceedsMaximum() {
        Exception exception = assertThrows(InvalidBidException.class, () -> {
            bidService.placeBid(auction, bidder, 20_000_000.0);
        });
        assertEquals("Bid amount is too large.", exception.getMessage());
    }
}
