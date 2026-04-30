package com.adjuge.model;

public class Bidder extends User implements Biddable {

    /** The number of auctions this bidder has won. */
    private int auctionsWon;

    /** The total amount of money this bidder has spent across all wins. */
    private double totalSpent;

    public Bidder(String id, String firstName, String lastName, String email, String password) {
        super(id, firstName, lastName, email, password);
        this.auctionsWon = 0;
        this.totalSpent = 0.0;
    }

    public int getAuctionsWon() {
        return auctionsWon;
    }

    public void setAuctionsWon(int auctionsWon) {
        this.auctionsWon = auctionsWon;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    // ── POLYMORPHISM overrides ───────────────────────────────────────────

    /** Returns "BIDDER" as this user's role. */
    @Override
    public String getRole() {
        return "BIDDER";
    }

    /** Bidders CAN place bids. */

    public boolean canBid() {
        return true;
    }

    /**
     * Returns a summary string with the bidder's name, wins, and total spending.
     */
    @Override
    public String printInfo() {
        return String.format("Bidder: %s | Won: %d | Spent: $%.2f",
                getFullName(), auctionsWon, totalSpent);
    }
}
