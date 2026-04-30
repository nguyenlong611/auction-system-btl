package com.adjuge.pattern;

import com.adjuge.model.Auction;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> auctions;
    private  AuctionManager() {
        auctions = new ArrayList<>();
    }
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {instance = new AuctionManager();}
        return instance;

    }
    public List<Auction> getAuctions() {
        return auctions;
    }
    public void addAuction(Auction auction) {
        auctions.add(auction);
    }
    public void removeAuction(Auction auction) {
        auctions.remove(auction);
    }
    public Auction searchAuction(String auctionId) {
        for (Auction auction : auctions) {
            if (auction.getId().equals(auctionId)) {
                return auction;
            }
        }
        return null;
    }
    public void startAuction(Auction auction) {
        auctions.add(auction);
    }


}
