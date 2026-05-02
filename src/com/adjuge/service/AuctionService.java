package com.adjuge.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.adjuge.model.*;
import com.adjuge.pattern.DataStore;

/**
 * Manages auction CRUD operations and queries.
 *
 * This service is the central place for creating auctions, finding active ones,
 * searching by keyword or category, and looking up auctions related to a
 * specific user (either as a bidder or as a seller).
 */
public class AuctionService {

    /**
     * Returns all auctions that are currently active (accepting bids).
     * An auction is active if its state is RUNNING and it has not yet expired.
     *
     * @return a list of active auctions
     */
    public List<Auction> getActiveAuctions() {
        return DataStore.getInstance().getAllAuctions().stream()
            .filter(a -> a.getState() == AuctionState.RUNNING && !a.isExpired())
            .collect(Collectors.toList());
    }

    /**
     * Looks up a single auction by its unique ID.
     *
     * @param id the auction ID (e.g. "a_3fa85f64")
     * @return the Auction object, or null if not found
     */
    public Auction getAuction(String id) {
        return DataStore.getInstance().getAuction(id);
    }

    /**
     * Searches active auctions by a text query and/or a category filter.
     *
     * How it works:
     * 1. Start with all active auctions
     * 2. If a category is provided, keep only auctions in that category
     * 3. If a text query is provided, keep only auctions whose name,
     *    description, or category label contains the query (case-insensitive)
     *
     * @param query    the search text (can be null or blank to skip text filtering)
     * @param category the category to filter by (can be null to skip category filtering)
     * @return a filtered list of matching auctions
     */
    public List<Auction> searchAuctions(String query, Category category) {
        // Start with all currently active auctions
        List<Auction> active = getActiveAuctions();

        // Step 1: Filter by category if one was selected
        if (category != null) {
            active = active.stream()
                .filter(a -> a.getItem().getCategory() == category)
                .collect(Collectors.toList());
        }

        // Step 2: Filter by text query if one was entered
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase(); // Case-insensitive comparison
            active = active.stream()
                .filter(a ->
                    a.getItem().getName().toLowerCase().contains(q) ||
                    a.getItem().getDescription().toLowerCase().contains(q) ||
                    a.getItem().getCategory().getDisplayName().toLowerCase().contains(q))
                .collect(Collectors.toList());
        }

        return active;
    }

    /**
     * Creates a new auction and saves it to the data store.
     *
     * The Item object should already be created (using ItemFactory in the controller layer)
     * before calling this method. This method handles generating the auction ID, setting
     * start/end times, and persisting the auction.
     *
     * @param item         the item being auctioned (already created by ItemFactory)
     * @param sellerId     the ID of the seller creating the auction
     * @param sellerName   the display name of the seller
     * @param durationDays how many days the auction should run
     * @return the newly created Auction object
     */
    public Auction createAuction(Item item, String sellerId, String sellerName, int durationDays, ValidatorType validatorType) {
        // Generate a unique auction ID like "a_3fa85f64"
        String id = "a_" + UUID.randomUUID().toString().substring(0, 8);

        // Calculate start time (now) and end time (now + duration)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusDays(durationDays);

        // Create the auction with the item's starting price as the initial bid level
        Auction auction = new Auction(id, item, sellerId, sellerName,
                                      item.getStartPrice(), now, endTime, validatorType);

        // Save to the data store so other parts of the app can find it
        DataStore.getInstance().addAuction(auction);

        return auction;
    }

    /**
     * Finds all auctions where a specific user has placed at least one bid.
     * Useful for showing a "My Bids" page.
     *
     * @param userId the ID of the user whose bids we want to find
     * @return a list of auctions the user has bid on
     */
    public List<Auction> getAuctionsWithUserBids(String userId) {
        return DataStore.getInstance().getAllAuctions().stream()
            // Keep an auction if ANY of its bids belong to this user
            .filter(a -> a.getBids().stream()
                .anyMatch(b -> b.getBidderId().equals(userId)))
            .collect(Collectors.toList());
    }

    /**
     * Finds all auctions listed by a specific seller.
     * Useful for showing a "My Listings" page.
     *
     * @param sellerId the ID of the seller
     * @return a list of auctions created by this seller
     */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        return DataStore.getInstance().getAllAuctions().stream()
            .filter(a -> a.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }

    /**
     * Returns the total number of bids placed across ALL auctions in the system.
     * Useful for dashboard statistics.
     *
     * @return the total bid count
     */
    public int getTotalBidCount() {
        return DataStore.getInstance().getAllAuctions().stream()
            .mapToInt(Auction::getBidCount)
            .sum();
    }
}
