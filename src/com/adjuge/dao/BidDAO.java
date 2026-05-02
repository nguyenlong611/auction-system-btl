package com.adjuge.dao;

import com.adjuge.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BidDAO — Data Access Object for BidTransaction operations.
 *
 * Handles saving and loading bid records from the database.
 * Each bid is linked to an auction and a bidder.
 */
public class BidDAO {

    private final Connection conn;

    public BidDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /** Insert a bid into the database */
    public void insert(BidTransaction bid) {
        String sql = "INSERT OR IGNORE INTO bids (id, auction_id, bidder_id, bidder_name, amount, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bid.getId());
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidderId());
            ps.setString(4, bid.getBidderName());
            ps.setDouble(5, bid.getAmount());
            ps.setString(6, bid.getTimestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[BidDAO] Insert failed: " + e.getMessage());
        }
    }

    /** Get all bids for a specific auction, ordered by timestamp */
    public List<BidTransaction> findByAuctionId(String auctionId) {
        List<BidTransaction> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY timestamp ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bids.add(buildBid(rs));
            }
        } catch (SQLException e) {
            System.err.println("[BidDAO] findByAuctionId failed: " + e.getMessage());
        }
        return bids;
    }

    /** Build a BidTransaction from a database row */
    private BidTransaction buildBid(ResultSet rs) throws SQLException {
        return new BidTransaction(
                rs.getString("id"),
                rs.getString("auction_id"),
                rs.getString("bidder_id"),
                rs.getString("bidder_name"),
                rs.getDouble("amount"),
                LocalDateTime.parse(rs.getString("timestamp"))
        );
    }
}
