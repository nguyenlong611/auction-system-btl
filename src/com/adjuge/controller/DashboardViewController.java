package com.adjuge.controller;

import com.adjuge.model.*;
import com.adjuge.service.AuctionService;
import com.adjuge.service.AuthService;
import com.adjuge.util.TimeUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sub-controller responsible for the Dashboard view (SRP).
 *
 * Demonstrates:
 *  - POLYMORPHISM: instanceof checks for Seller-specific behaviour
 *  - Generics: List<Auction> filtered through Stream API
 *  - Lambda expressions in stream operations
 */
public class DashboardViewController {

    private final VBox           dashboardContent;
    private final AuthService    authService;
    private final AuctionService auctionService;
    private final Runnable       onRedirectToSignIn;

    public DashboardViewController(
            VBox dashboardContent,
            AuthService authService, AuctionService auctionService,
            Runnable onRedirectToSignIn) {
        this.dashboardContent   = dashboardContent;
        this.authService        = authService;
        this.auctionService     = auctionService;
        this.onRedirectToSignIn = onRedirectToSignIn;
    }

    /** Rebuild the dashboard for the currently logged-in user */
    public void renderDashboard() {
        User user = authService.getCurrentUser();
        if (user == null) { onRedirectToSignIn.run(); return; }
        if (dashboardContent == null) return;
        dashboardContent.getChildren().clear();

        // Generics: List<Auction>
        List<Auction> myBidAuctions = auctionService.getAuctionsWithUserBids(user.getId());
        List<Auction> myListings    = auctionService.getAuctionsBySeller(user.getId());
        long activeBids = myBidAuctions.stream().filter(a -> !a.isExpired()).count(); // Lambda

        // POLYMORPHISM: instanceof check for Seller-specific method
        if (user instanceof Seller) ((Seller) user).setTotalListings(myListings.size());

        // Generics + Lambda: filter to won auctions
        List<Auction> wonAuctions = myBidAuctions.stream()
                .filter(a -> a.isExpired() && user.getId().equals(a.getHighestBidderId()))
                .collect(Collectors.toList());
        double totalSpent = wonAuctions.stream().mapToDouble(Auction::getCurrentHighestBid).sum();

        dashboardContent.getChildren().addAll(
                buildProfileCard(user),
                buildStatsRow(activeBids, wonAuctions.size(), totalSpent, myListings.size()),
                buildActivityColumns(user, myBidAuctions, myListings));
    }

    // ── Private builders ──────────────────────────────────────────────

    private HBox buildProfileCard(User user) {
        String initials = user.getFirstName().substring(0, 1).toUpperCase()
                        + user.getLastName().substring(0, 1).toUpperCase();
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("profile-avatar");
        avatar.setMinSize(64, 64); avatar.setMaxSize(64, 64);
        Label avatarText = new Label(initials); avatarText.getStyleClass().add("profile-avatar-text");
        avatar.getChildren().add(avatarText);

        Label nameLabel   = new Label(user.getFirstName() + " " + user.getLastName());
        nameLabel.getStyleClass().add("profile-name");
        Label emailLabel  = new Label(user.getEmail()); emailLabel.getStyleClass().add("profile-email");
        Label memberLabel = new Label("Member since " + TimeUtil.formatDate(user.getCreatedAt()));
        memberLabel.getStyleClass().add("profile-meta");
        Label roleLabel   = new Label(user.getRole().toString()); roleLabel.getStyleClass().add("role-badge");
        HBox badgeRow = new HBox(8, roleLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // POLYMORPHISM: instanceof to call Seller-specific method
        if (user instanceof Seller && ((Seller) user).isVerified()) {
            Label verifiedBadge = new Label("✓ Verified");
            verifiedBadge.getStyleClass().add("verified-badge");
            badgeRow.getChildren().add(verifiedBadge);
        }

        VBox profileInfo = new VBox(5, nameLabel, emailLabel, memberLabel, badgeRow);
        profileInfo.setAlignment(Pos.CENTER_LEFT);
        HBox profileCard = new HBox(20, avatar, profileInfo);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.getStyleClass().add("profile-card");
        VBox.setMargin(profileCard, new Insets(0, 0, 20, 0));
        return profileCard;
    }

    private HBox buildStatsRow(long activeBids, int won, double totalSpent, int listings) {
        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                statCard("ACTIVE BIDS",  String.valueOf(activeBids),          "Currently bidding", false),
                statCard("AUCTIONS WON", String.valueOf(won),                  "Items secured",     true),
                statCard("TOTAL SPENT",  TimeUtil.formatCurrency(totalSpent),  "On won items",      false),
                statCard("MY LISTINGS",  String.valueOf(listings),             "Items for sale",    false));
        for (javafx.scene.Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        VBox.setMargin(statsRow, new Insets(0, 0, 20, 0));
        return statsRow;
    }

    private HBox buildActivityColumns(User user, List<Auction> bidAuctions, List<Auction> listings) {
        VBox leftCol  = buildBidActivitySection(user, bidAuctions);
        VBox rightCol = buildListingsSection(listings);
        HBox.setHgrow(leftCol, Priority.ALWAYS); HBox.setHgrow(rightCol, Priority.ALWAYS);
        return new HBox(16, leftCol, rightCol);
    }

    private VBox statCard(String label, String value, String sub, boolean gold) {
        VBox card = new VBox(6); card.getStyleClass().add("stat-card"); card.setPadding(new Insets(20));
        Label l = new Label(label); l.getStyleClass().add("stat-card-label");
        Label v = new Label(value); v.getStyleClass().add(gold ? "stat-card-value-gold" : "stat-card-value");
        Label s = new Label(sub);   s.getStyleClass().add("stat-card-sub");
        card.getChildren().addAll(l, v, s);
        return card;
    }

    private VBox buildBidActivitySection(User user, List<Auction> bidAuctions) {
        VBox section = new VBox(12); section.getStyleClass().add("dash-section-card");
        Label title = new Label("Bid Activity"); title.getStyleClass().add("dash-section-title");
        section.getChildren().add(title);
        if (bidAuctions.isEmpty()) {
            Label empty = new Label("You haven't placed any bids yet.");
            empty.getStyleClass().add("dash-empty-state"); section.getChildren().add(empty);
            return section;
        }
        for (Auction a : bidAuctions) {
            // Generics + Lambda: find user's max bid
            double myBid = a.getBids().stream()
                    .filter(b -> user.getId().equals(b.getBidderId()))
                    .mapToDouble(b -> b.getAmount()).max().orElse(0);
            boolean isWinning = user.getId().equals(a.getHighestBidderId());
            boolean isExpired  = a.isExpired();
            String statusText, statusStyle;
            if      (isExpired && isWinning) { statusText = "WON";     statusStyle = "status-won"; }
            else if (isExpired)              { statusText = "LOST";    statusStyle = "status-closed"; }
            else if (isWinning)              { statusText = "WINNING"; statusStyle = "status-winning"; }
            else                             { statusText = "OUTBID";  statusStyle = "status-outbid"; }

            Label itemTitle   = new Label(truncate(a.getItem().getName(), 38)); itemTitle.getStyleClass().add("bid-row-title");
            Label myBidLabel  = new Label("Your bid: " + TimeUtil.formatCurrency(myBid)); myBidLabel.getStyleClass().add("bid-row-meta");
            Label currLabel   = new Label("Current: " + TimeUtil.formatCurrency(a.getCurrentHighestBid())); currLabel.getStyleClass().add("bid-row-current");
            Label statusBadge = new Label(statusText); statusBadge.getStyleClass().addAll("status-badge", statusStyle);
            Label timeLabel   = new Label(isExpired ? "Ended" : TimeUtil.shortTimer(a.getEndTime())); timeLabel.getStyleClass().add("bid-row-time");

            Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
            HBox topRow = new HBox(8, itemTitle, gap, statusBadge); topRow.setAlignment(Pos.CENTER_LEFT);
            Region gap2 = new Region(); HBox.setHgrow(gap2, Priority.ALWAYS);
            HBox bottomRow = new HBox(16, myBidLabel, currLabel, gap2, timeLabel); bottomRow.setAlignment(Pos.CENTER_LEFT);
            VBox row = new VBox(4, topRow, bottomRow); row.getStyleClass().add("bid-row");
            section.getChildren().add(row);
        }
        return section;
    }

    private VBox buildListingsSection(List<Auction> listings) {
        VBox section = new VBox(12); section.getStyleClass().add("dash-section-card");
        Label title = new Label("My Listings"); title.getStyleClass().add("dash-section-title");
        section.getChildren().add(title);
        if (listings.isEmpty()) {
            Label empty = new Label("No listings yet. Use \"List an Item\" to get started.");
            empty.getStyleClass().add("dash-empty-state"); empty.setWrapText(true);
            section.getChildren().add(empty); return section;
        }
        for (Auction a : listings) {
            boolean isExpired  = a.isExpired();
            String statusText  = isExpired ? "CLOSED" : "LIVE";
            String statusStyle = isExpired ? "status-closed" : "status-winning";
            Label itemTitle   = new Label(truncate(a.getItem().getName(), 34)); itemTitle.getStyleClass().add("bid-row-title");
            Label statusBadge = new Label(statusText); statusBadge.getStyleClass().addAll("status-badge", statusStyle);
            Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
            HBox topRow = new HBox(8, itemTitle, gap, statusBadge); topRow.setAlignment(Pos.CENTER_LEFT);
            Label bidsLabel  = new Label(a.getBidCount() + " bids"); bidsLabel.getStyleClass().add("bid-row-meta");
            Label priceLabel = new Label("Top: " + TimeUtil.formatCurrency(a.getCurrentHighestBid())); priceLabel.getStyleClass().add("bid-row-current");
            Label timeLabel  = new Label(isExpired ? "Ended" : TimeUtil.shortTimer(a.getEndTime())); timeLabel.getStyleClass().add("bid-row-time");
            Region gap2 = new Region(); HBox.setHgrow(gap2, Priority.ALWAYS);
            HBox bottomRow = new HBox(16, bidsLabel, priceLabel, gap2, timeLabel); bottomRow.setAlignment(Pos.CENTER_LEFT);
            VBox row = new VBox(4, topRow, bottomRow); row.getStyleClass().add("bid-row");
            section.getChildren().add(row);
        }
        return section;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
