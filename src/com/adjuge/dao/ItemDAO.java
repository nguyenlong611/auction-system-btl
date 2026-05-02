package com.adjuge.dao;

import com.adjuge.model.*;

import java.sql.*;

/**
 * ItemDAO — Data Access Object for Item operations.
 *
 * Stores all Item subtypes in one table. The 'category' column determines
 * which subclass to create when reading back (Factory-like polymorphism).
 *
 * Extra fields (brand, artist, make, etc.) are stored in extra1-extra4 columns
 * since each subtype has different fields.
 */
public class ItemDAO {

    private final Connection conn;

    public ItemDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /** Insert an item into the database */
    public void insert(Item item) {
        String sql = "INSERT INTO items (id, name, description, start_price, image_url, condition, category, extra1, extra2, extra3, extra4, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getStartPrice());
            ps.setString(5, item.getImageUrl());
            ps.setString(6, item.getCondition());
            ps.setString(7, item.getCategory().name());  // Store enum name

            // Store subtype-specific fields in extra columns
            String[] extras = getExtras(item);
            ps.setString(8, extras[0]);
            ps.setString(9, extras[1]);
            ps.setString(10, extras[2]);
            ps.setString(11, extras[3]);
            ps.setString(12, item.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ItemDAO] Insert failed: " + e.getMessage());
        }
    }

    /** Find an item by ID — returns the correct subtype */
    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return buildItem(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ItemDAO] findById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract subtype-specific fields into extra1-extra4.
     *
     * POLYMORPHISM: we check the actual type with instanceof
     * to get the specific fields each subtype has.
     */
    private String[] getExtras(Item item) {
        String[] extras = {"", "", "", ""};
        if (item instanceof Electronics e) {
            extras[0] = e.getBrand();
            extras[1] = e.getModel();
            extras[2] = String.valueOf(e.getWarrantyMonths());
        } else if (item instanceof Art a) {
            extras[0] = a.getArtist();
            extras[1] = String.valueOf(a.getYear());
            extras[2] = a.getMedium();
        } else if (item instanceof Vehicle v) {
            extras[0] = String.valueOf(v.getYearMade());
            extras[1] = v.getMake();
            extras[2] = v.getVehicleModel();
            extras[3] = String.valueOf(v.getMileage());
        }
        return extras;
    }

    /**
     * Build the correct Item subtype from a database row.
     *
     * FACTORY-LIKE: reads the category column and creates the matching class.
     */
    private Item buildItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        double price = rs.getDouble("start_price");
        String image = rs.getString("image_url");
        String condition = rs.getString("condition");
        String catStr = rs.getString("category");
        String e1 = rs.getString("extra1");
        String e2 = rs.getString("extra2");
        String e3 = rs.getString("extra3");
        String e4 = rs.getString("extra4");

        Category category = Category.valueOf(catStr);

        return switch (category) {
            case ELECTRONICS -> new Electronics(id, name, desc, price, image, condition,
                    e1, e2, parseIntSafe(e3));
            case ART_COLLECTIBLES -> new Art(id, name, desc, price, image, condition,
                    e1, parseIntSafe(e2), e3);
            case VEHICLES -> new Vehicle(id, name, desc, price, image, condition,
                    parseIntSafe(e1), e2, e3, parseIntSafe(e4));
            default -> new Art(id, name, desc, price, image, condition, "Unknown", 0, "Other");
        };
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
