package com.adjuge.dao;

import java.sql.*;

/**
 * DatabaseManager — SINGLETON that manages the SQLite database connection.
 *
 * Creates the database file (adjuge.db) and all tables on first run.
 * Only ONE connection exists for the whole app.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    // Database file is stored next to the app
    private static final String DB_URL = "jdbc:sqlite:adjuge.db";

    private DatabaseManager() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);

            // Enable WAL mode for better concurrent read performance
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA foreign_keys=ON");
            }

            createTables();
            System.out.println("[Database] Connected to adjuge.db");
        } catch (Exception e) {
            System.err.println("[Database] Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Get the single instance (Singleton Pattern) */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /** Get the database connection */
    public Connection getConnection() {
        return connection;
    }

    /** Create all tables if they don't exist */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // Users table — stores Bidder, Seller, Admin
            // 'role' column determines the User subtype (polymorphism at DB level)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id          TEXT PRIMARY KEY,
                    first_name  TEXT NOT NULL,
                    last_name   TEXT NOT NULL,
                    email       TEXT NOT NULL UNIQUE,
                    password    TEXT NOT NULL,
                    role        TEXT NOT NULL DEFAULT 'SELLER',
                    verified    INTEGER DEFAULT 0,
                    created_at  TEXT NOT NULL
                )
            """);

            // Items table — stores all Item subtypes
            // 'category' determines the subtype, 'specifics' holds type-specific JSON
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id          TEXT PRIMARY KEY,
                    name        TEXT NOT NULL,
                    description TEXT,
                    start_price REAL NOT NULL,
                    image_url   TEXT,
                    condition   TEXT,
                    category    TEXT NOT NULL,
                    extra1      TEXT DEFAULT '',
                    extra2      TEXT DEFAULT '',
                    extra3      TEXT DEFAULT '',
                    extra4      TEXT DEFAULT '',
                    created_at  TEXT NOT NULL
                )
            """);

            // Auctions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id                TEXT PRIMARY KEY,
                    item_id           TEXT NOT NULL,
                    seller_id         TEXT NOT NULL,
                    seller_name       TEXT NOT NULL,
                    state             TEXT NOT NULL DEFAULT 'RUNNING',
                    current_highest   REAL NOT NULL,
                    highest_bidder_id TEXT DEFAULT '',
                    start_time        TEXT NOT NULL,
                    end_time          TEXT NOT NULL,
                    FOREIGN KEY (item_id) REFERENCES items(id),
                    FOREIGN KEY (seller_id) REFERENCES users(id)
                )
            """);

            // Bid transactions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bids (
                    id          TEXT PRIMARY KEY,
                    auction_id  TEXT NOT NULL,
                    bidder_id   TEXT NOT NULL,
                    bidder_name TEXT NOT NULL,
                    amount      REAL NOT NULL,
                    timestamp   TEXT NOT NULL,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    FOREIGN KEY (bidder_id) REFERENCES users(id)
                )
            """);
        }
    }

    /** Check if the database has any users (to know if we need to seed) */
    public boolean isEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            return true;
        }
    }

    /** Close the connection (called on app shutdown) */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[Database] Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
