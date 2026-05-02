package com.adjuge.dao;

import com.adjuge.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO — Data Access Object for User operations.
 *
 * Handles all SQL queries related to users (Bidder, Seller, Admin).
 * Uses POLYMORPHISM: reads the 'role' column to create the correct User subtype.
 */
public class UserDAO {

    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /** Insert a new user into the database */
    public void insert(User user) {
        String sql = "INSERT INTO users (id, first_name, last_name, email, password, role, verified, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getLastName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPassword());
            ps.setString(6, user.getRole());        // POLYMORPHISM: getRole() returns different string per subtype
            ps.setInt(7, (user instanceof Seller s && s.isVerified()) ? 1 : 0);
            ps.setString(8, user.getCreatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO] Insert failed: " + e.getMessage());
        }
    }

    /** Find a user by email — returns the correct subtype (Bidder/Seller/Admin) */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return buildUser(rs);  // POLYMORPHISM: creates correct subtype
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] findByEmail failed: " + e.getMessage());
        }
        return null;
    }

    /** Find a user by ID */
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return buildUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] findById failed: " + e.getMessage());
        }
        return null;
    }

    /** Get all users */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(buildUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] findAll failed: " + e.getMessage());
        }
        return users;
    }

    /**
     * Build the correct User subtype from a database row.
     *
     * POLYMORPHISM in action: the 'role' column determines which class to create.
     * The returned object is always stored as User, but the actual type varies.
     */
    private User buildUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String role = rs.getString("role");

        // FACTORY-LIKE logic: create the right subtype based on role
        User user = switch (role) {
            case "BIDDER" -> new Bidder(id, firstName, lastName, email, password);
            case "ADMIN"  -> new Admin(id, firstName, lastName, email, password);
            default       -> new Seller(id, firstName, lastName, email, password);
        };

        return user;
    }
}
