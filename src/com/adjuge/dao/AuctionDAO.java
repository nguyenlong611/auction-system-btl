package com.adjuge.dao;

import com.adjuge.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionDAO — Data Access Object for Auction operations.
 *
 * Handles saving/loading auctions from the database.
 * Uses ItemDAO to load the Item (polymorphic — could be any subtype).
 * Uses BidDAO to load all bids for each auction.
 */
public class AuctionDAO {

    private final Connection conn;
    private final ItemDAO itemDAO;
    private final BidDAO bidDAO;

    public AuctionDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
        this.itemDAO = new ItemDAO();
        this.bidDAO = new BidDAO();
    }

    /** Insert a new auction (also inserts the item) */
    public void insert(Auction auction) {
        // First, save the item
        itemDAO.insert(auction.getItem());

        String sql = "INSERT INTO auctions (id, item_id, seller_id, seller_name, state, current_highest, highest_bidder_id, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItem().getId());
            ps.setString(3, auction.getSellerId());
            ps.setString(4, auction.getSellerName());
            ps.setString(5, auction.getState().name());
            ps.setDouble(6, auction.getCurrentHighestBid());
            ps.setString(7, auction.getHighestBidderId() != null ? auction.getHighestBidderId() : "");
            ps.setString(8, auction.getStartTime().toString());
            ps.setString(9, auction.getEndTime().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Insert failed: " + e.getMessage());
        }

        // Save all existing bids
        for (BidTransaction bid : auction.getBids()) {
            bidDAO.insert(bid);
        }
    }

    /** Update auction state and current bid info */
    public void update(Auction auction) {
        String sql = "UPDATE auctions SET state = ?, current_highest = ?, highest_bidder_id = ?, end_time = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getState().name());
            ps.setDouble(2, auction.getCurrentHighestBid());
            ps.setString(3, auction.getHighestBidderId() != null ? auction.getHighestBidderId() : "");
            ps.setString(4, auction.getEndTime().toString());
            ps.setString(5, auction.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Update failed: " + e.getMessage());
        }
    }

    /** Find an auction by ID — loads the item and all bids */
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return buildAuction(rs);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] findById failed: " + e.getMessage());
        }
        return null;
    }

    /** Get all auctions */
    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Auction a = buildAuction(rs);
                if (a != null) auctions.add(a);
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] findAll failed: " + e.getMessage());
        }
        return auctions;
    }

    /** Remove an auction */
    public void delete(String id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bids WHERE auction_id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Delete bids failed: " + e.getMessage());
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM auctions WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuctionDAO] Delete failed: " + e.getMessage());
        }
    }

    /**
     * Build an Auction from a database row.
     * Loads the Item (polymorphic) and all BidTransactions.
     */
    private Auction buildAuction(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("item_id");
        String sellerId = rs.getString("seller_id");
        String sellerName = rs.getString("seller_name");
        String stateStr = rs.getString("state");
        LocalDateTime startTime = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime endTime = LocalDateTime.parse(rs.getString("end_time"));
        double currentHighest = rs.getDouble("current_highest");
        ValidatorType validatorType = ValidatorType.valueOf(rs.getString("validator_type"));

        // Load the item — POLYMORPHISM: could return Electronics, Art, Vehicle, etc.
        Item item = itemDAO.findById(itemId);
        if (item == null) return null;

        Auction auction = new Auction(id, item, sellerId, sellerName,
                item.getStartPrice(), startTime, endTime, validatorType);

        // Load all bids for this auction
        List<BidTransaction> bids = bidDAO.findByAuctionId(id);
        for (BidTransaction bid : bids) {
            auction.addBid(bid);
        }

        return auction;
    }
}
