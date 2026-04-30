package com.adjuge.model;

public class Seller extends User implements Biddable, Sellable {

    /** Whether this seller has been verified by the platform. */
    private boolean verified;

    /** The total number of items this seller has listed. */
    private int totalListings;

    public Seller(String id, String firstName, String lastName, String email, String password) {
        super(id, firstName, lastName, email, password);
        this.verified = false;
        this.totalListings = 0;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getTotalListings() {
        return totalListings;
    }

    public void setTotalListings(int totalListings) {
        this.totalListings = totalListings;
    }

    // ── POLYMORPHISM overrides ───────────────────────────────────────────

    /** Returns "SELLER" as this user's role. */
    @Override
    public String getRole() {
        return "SELLER";
    }

    /** Sellers CAN place bids on other sellers' auctions. */
    public boolean canBid() {
        return true;
    }

    /** Sellers CAN list items for sale. */
    public boolean canSell() {
        return true;
    }

    /**
     * Returns a summary string with the seller's name, verification status, and listing count.
     */
    @Override
    public String printInfo() {
        return String.format("Seller: %s | Verified: %s | Listings: %d",
                getFullName(), verified ? "Y" : "N", totalListings);
    }
}
