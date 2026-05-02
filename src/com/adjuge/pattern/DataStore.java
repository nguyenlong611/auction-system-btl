package com.adjuge.pattern;

import com.adjuge.model.*;
import com.adjuge.dao.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * SINGLETON PATTERN — Central data store for the entire application.
 *
 * Only ONE instance of this class ever exists. All controllers and services
 * access the same DataStore to read/write auctions and users.
 *
 * How it works:
 *   1. The constructor is private — nobody can call "new DataStore()".
 *   2. The only way to get the instance is via DataStore.getInstance().
 *   3. The first call creates the instance; every later call returns the same one.
 *   4. "synchronized" ensures thread safety (only one thread can create it).
 */
public class DataStore {

    // The single instance (starts as null until first access)
    private static DataStore instance;

    // Thread-safe maps to store auctions and users by their IDs
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();

    // DAOs for database persistence
    private UserDAO userDAO;
    private AuctionDAO auctionDAO;
    private BidDAO bidDAO;

    // Private constructor — initializes DAOs and loads from DB
    private DataStore() {
        userDAO = new UserDAO();
        auctionDAO = new AuctionDAO();
        bidDAO = new BidDAO();
    }

    /**
     * Returns the single DataStore instance, creating it if needed.
     * The "synchronized" keyword prevents two threads from creating
     * two instances at the same time.
     */
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // ---------------------------------------------------------------
    //  Auction CRUD methods
    // ---------------------------------------------------------------

    /** Store an auction in memory AND in the database. */
    public void addAuction(Auction a) {
        auctions.put(a.getId(), a);
        auctionDAO.insert(a);
    }

    /** Retrieve an auction by ID, or null if not found. */
    public Auction getAuction(String id) {
        return auctions.get(id);
    }

    /** Get every auction as a list. */
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    /** Remove an auction by ID. */
    public void removeAuction(String id) {
        auctions.remove(id);
    }

    // ---------------------------------------------------------------
    //  User CRUD methods
    // ---------------------------------------------------------------

    /** Store a user in memory AND in the database. */
    public void addUser(User u) {
        users.put(u.getId(), u);
        userDAO.insert(u);
    }

    /** Retrieve a user by ID, or null if not found. */
    public User getUser(String id) {
        return users.get(id);
    }

    /**
     * Look up a user by email address (case-insensitive).
     * Returns null if no user has that email.
     */
    public User getUserByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    /** Get every user as a list. */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // ---------------------------------------------------------------
    //  Seed data — pre-populates the 8 demo auctions from the UI
    // ---------------------------------------------------------------

    /**
     * Load existing data from the database, OR seed fresh data on first run.
     * Called once at app startup.
     */
    public void seedData() {
        // Check if the database already has data (from a previous run)
        if (!DatabaseManager.getInstance().isEmpty()) {
            System.out.println("[DataStore] Loading existing data from database...");
            loadFromDatabase();
            System.out.println("[DataStore] Loaded " + users.size() + " users, " + auctions.size() + " auctions.");
            return;
        }

        System.out.println("[DataStore] First run — seeding demo data...");

        // Clear any existing in-memory data
        users.clear();
        auctions.clear();

        // ---- Create demo users ----
        Seller demoUser = new Seller("u_demo", "Alex", "Morgan",
                "demo@adjuge.com", "password123");
        demoUser.setVerified(true);
        Seller s1 = new Seller("u_s1", "James", "Thompson",
                "james@example.com", "pass123");
        s1.setVerified(true);
        Bidder s2 = new Bidder("u_s2", "Mike", "Rodriguez",
                "mike@example.com", "pass123");
        Bidder s3 = new Bidder("u_s3", "Sarah", "Kim",
                "sarah@example.com", "pass123");

        addUser(demoUser);
        addUser(s1);
        addUser(s2);
        addUser(s3);

        // Current time — all auction dates are relative to "now"
        LocalDateTime now = LocalDateTime.now();

        // ---- Create the 8 auction items ----

        // Item 3: Banksy print (Art category)
        Art banksy = new Art("i3",
                "Authenticated Banksy Print — \"Girl with Balloon\"",
                "Authenticated Banksy screen print, signed and numbered 147/600. "
                        + "Comes with Certificate of Authenticity from Pest Control.",
                5000, "https://picsum.photos/seed/artprint/800/600", "Mint",
                "Banksy", 2004, "Screen Print");

        // Item 4: MacBook Pro (Electronics category)
        Electronics macbook = new Electronics("i4",
                "Apple MacBook Pro 16\" M3 Max — Space Black",
                "Brand new, factory sealed MacBook Pro 16\" with M3 Max chip, "
                        + "48GB unified RAM, 2TB SSD. Space Black finish.",
                2000, "https://picsum.photos/seed/macbookpro/800/600", "Mint",
                "Apple", "MacBook Pro 16\" M3 Max", 12);

        // Item 5: Classic Mustang (Vehicle category)
        Vehicle mustang = new Vehicle("i5",
                "1965 Ford Mustang Fastback — Restored",
                "Fully restored 1965 Ford Mustang Fastback in Highland Green. "
                        + "Numbers-matching 289 V8, 4-speed manual.",
                45000, "https://picsum.photos/seed/mustangcar/800/600", "Excellent",
                1965, "Ford", "Mustang Fastback", 45000);

        // Item 8: Pokemon booster box (Art/Collectibles category)
        Art pokemon = new Art("i8",
                "Pokémon Base Set Booster Box — Unlimited",
                "Factory sealed Pokémon Trading Card Game Base Set Unlimited Edition "
                        + "booster box. 36 packs. Original plastic wrap intact.",
                5000, "https://picsum.photos/seed/pokemonbox/800/600", "Mint",
                "Wizards of the Coast", 1999, "Trading Cards");

        // ---- Create auctions with historical bid data ----

        // Auction 3: Banksy print — listed by Sarah, 6 bids (most active)
        Auction a3 = new Auction("a3", banksy, "u_s3", "Sarah K.",
                5000, now.minusDays(6), now.plusDays(1).plusHours(6),ValidatorType.STANDARD);
        a3.addBid(new BidTransaction("b9", "a3", "u_s1", "James T.", 5000, now.minusDays(6)));
        a3.addBid(new BidTransaction("b10", "a3", "u_demo", "Alex M.", 7500, now.minusDays(5)));
        a3.addBid(new BidTransaction("b11", "a3", "u_s2", "Mike R.", 10000, now.minusDays(4)));
        a3.addBid(new BidTransaction("b12", "a3", "u_s1", "James T.", 13000, now.minusDays(3)));
        a3.addBid(new BidTransaction("b13", "a3", "u_demo", "Alex M.", 15000, now.minusDays(2)));
        a3.addBid(new BidTransaction("b14", "a3", "u_s1", "James T.", 18750, now.minusHours(12)));

        // Auction 4: MacBook — listed by Alex (demo user), 3 bids
        Auction a4 = new Auction("a4", macbook, "u_demo", "Alex M.",
                2000, now.minusDays(1), now.plusDays(3), ValidatorType.STEP_PRICE);
        a4.addBid(new BidTransaction("b15", "a4", "u_s1", "James T.", 2000, now.minusDays(1)));
        a4.addBid(new BidTransaction("b16", "a4", "u_s2", "Mike R.", 2500, now.minusHours(18)));
        a4.addBid(new BidTransaction("b17", "a4", "u_s3", "Sarah K.", 3100, now.minusHours(8)));

        // Auction 5: Mustang — listed by Mike, 3 bids
        Auction a5 = new Auction("a5", mustang, "u_s2", "Mike R.",
                45000, now.minusDays(1), now.plusDays(6),ValidatorType.STEP_PRICE);
        a5.addBid(new BidTransaction("b18", "a5", "u_s3", "Sarah K.", 45000, now.minusDays(1)));
        a5.addBid(new BidTransaction("b19", "a5", "u_s1", "James T.", 55000, now.minusHours(20)));
        a5.addBid(new BidTransaction("b20", "a5", "u_s3", "Sarah K.", 67500, now.minusHours(10)));

        // Auction 8: Pokemon booster box — listed by James, 4 bids
        Auction a8 = new Auction("a8", pokemon, "u_s1", "James T.",
                5000, now.minusDays(4), now.plusDays(2).plusHours(14),ValidatorType.STANDARD);
        a8.addBid(new BidTransaction("b30", "a8", "u_s3", "Sarah K.", 5000, now.minusDays(4)));
        a8.addBid(new BidTransaction("b31", "a8", "u_demo", "Alex M.", 6500, now.minusDays(3)));
        a8.addBid(new BidTransaction("b32", "a8", "u_s2", "Mike R.", 7800, now.minusDays(2)));
        a8.addBid(new BidTransaction("b33", "a8", "u_s3", "Sarah K.", 9750, now.minusDays(1)));

        // Add all 8 auctions to the store
        for (Auction a : List.of(a3, a4, a5,a8)) {
            addAuction(a);
        }
    }

    // ---------------------------------------------------------------
    //  Database persistence methods
    // ---------------------------------------------------------------

    /** Load all users and auctions from the SQLite database into memory */
    private void loadFromDatabase() {
        users.clear();
        auctions.clear();

        // Load users
        for (User u : userDAO.findAll()) {
            users.put(u.getId(), u);
        }

        // Load auctions (each auction loads its Item + Bids via DAOs)
        for (Auction a : auctionDAO.findAll()) {
            auctions.put(a.getId(), a);
        }
    }

    /** Save a new bid to the database (called after a bid is placed) */
    public void saveBid(BidTransaction bid, Auction auction) {
        bidDAO.insert(bid);
        auctionDAO.update(auction);
    }

    /** Update an auction in the database (e.g., state change) */
    public void updateAuction(Auction auction) {
        auctionDAO.update(auction);
    }
}
