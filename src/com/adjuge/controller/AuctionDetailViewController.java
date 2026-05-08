package com.adjuge.controller;

import com.adjuge.exception.AuctionClosedException;
import com.adjuge.exception.InvalidBidException;
import com.adjuge.model.*;
import com.adjuge.service.AuthService;
import com.adjuge.service.BidService;
import com.adjuge.util.TimeUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Sub-controller responsible for the Auction Detail view (SRP).
 *
 * Demonstrates:
 *  - POLYMORPHISM: item.getCategory().getDisplayName() / getItemSpecifics() differ per subtype
 *  - Exception Handling: multi-catch in onPlaceBid()
 *  - Lambda expressions for button event handlers
 *  - Generics: List<BidTransaction> sorted via Stream + lambda
 */
public class AuctionDetailViewController {

    @FunctionalInterface
    public interface AlertCallback { void show(String title, String message); }

    private final HBox           auctionDetailContent;
    private final AuthService    authService;
    private final BidService     bidService;
    private final Runnable       onShowSignIn;
    private final AlertCallback  onAlert;
    private Auction              currentAuction;

    public AuctionDetailViewController(
            HBox auctionDetailContent,
            AuthService authService, BidService bidService,
            Runnable onShowSignIn, AlertCallback onAlert) {
        this.auctionDetailContent = auctionDetailContent;
        this.authService = authService;
        this.bidService  = bidService;
        this.onShowSignIn = onShowSignIn;
        this.onAlert = onAlert;
    }

    public void setCurrentAuction(Auction auction) { this.currentAuction = auction; }

    /** Rebuild the entire auction detail UI */
    public void renderAuctionDetail() {
        if (currentAuction == null || auctionDetailContent == null) return;
        auctionDetailContent.getChildren().clear();
        Auction a    = currentAuction;
        Item    item = a.getItem(); // POLYMORPHIC
        auctionDetailContent.getChildren().addAll(buildLeftColumn(a, item), buildRightColumn(a, item));
        auctionDetailContent.setSpacing(32);
    }

    /**
     * Place a bid — multi-catch Exception Handling.
     *  - NumberFormatException  → non-numeric input
     *  - InvalidBidException    → custom: bid too low
     *  - AuctionClosedException → custom: auction ended
     */
    public void onPlaceBid(TextField bidInput) {
        User user = authService.getCurrentUser();
        if (user == null || currentAuction == null) return;
        try {
            double amount = Double.parseDouble(bidInput.getText().trim());
            bidService.placeBid(currentAuction, user, amount); // synchronized
        } catch (NumberFormatException e) {
            onAlert.show("Invalid Input", "Please enter a valid number.");
        } catch (InvalidBidException e) {
            onAlert.show("Invalid Bid", e.getMessage());
        } catch (AuctionClosedException e) {
            onAlert.show("Auction Closed", e.getMessage());
        }
    }

    // ── Private builders ──────────────────────────────────────────────

    private VBox buildLeftColumn(Auction a, Item item) {
        VBox leftCol = new VBox();
        leftCol.setSpacing(0);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("auction-image-main");
        imagePane.setMinHeight(450); imagePane.setPrefHeight(450);
        try {
            ImageView img = new ImageView(new Image(item.getImageUrl(), 900, 450, false, true, true));
            img.setFitWidth(900); img.setFitHeight(450); img.setPreserveRatio(false);
            imagePane.getChildren().add(img);
        } catch (Exception e) { /* fallback */ }

        HBox metaTags = new HBox(12);
        metaTags.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(metaTags, new Insets(16, 0, 16, 0));
        Label catTag  = new Label(item.getCategory().getDisplayName()); catTag.getStyleClass().add("meta-tag-gold"); // POLYMORPHISM via Category enum
        Label condTag = new Label("Condition: " + item.getCondition()); condTag.getStyleClass().add("meta-tag");
        Label specTag = new Label(item.getItemSpecifics()); specTag.getStyleClass().add("meta-tag");       // POLYMORPHISM
        metaTags.getChildren().addAll(catTag, condTag, specTag);

        Label titleLabel = new Label(item.getName());
        titleLabel.getStyleClass().add("detail-title"); titleLabel.setWrapText(true);

        Label descLabel = new Label(item.getDescription());
        descLabel.getStyleClass().add("detail-description"); descLabel.setWrapText(true);
        VBox.setMargin(descLabel, new Insets(16, 0, 32, 0));

        leftCol.getChildren().addAll(imagePane, metaTags, titleLabel, descLabel, buildBidHistory(a));
        return leftCol;
    }

    private VBox buildBidHistory(Auction a) {
        Label histTitle = new Label("Bid History"); histTitle.getStyleClass().add("bid-history-title");
        Label histCount = new Label("(" + a.getBidCount() + " bid" + (a.getBidCount() != 1 ? "s" : "") + ")");
        histCount.getStyleClass().add("bid-history-count");
        HBox histHeader = new HBox(8, histTitle, histCount);
        histHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(histHeader, new Insets(0, 0, 16, 0));

        VBox histList = new VBox(6);
        histList.getStyleClass().add("bid-history-list");
        // Generics + Lambda: List<BidTransaction> sorted descending by timestamp
        List<BidTransaction> sorted = a.getBids().stream()
                .sorted((x, y) -> y.getTimestamp().compareTo(x.getTimestamp()))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            BidTransaction bid = sorted.get(i);
            boolean isTop = (i == 0);
            HBox entry = new HBox();
            entry.getStyleClass().add(isTop ? "bid-entry-top" : "bid-entry");
            entry.setAlignment(Pos.CENTER_LEFT);
            entry.setPadding(new Insets(9, 12, 9, 12));
            VBox userInfo = new VBox();
            Label userName  = new Label(bid.getBidderName() + (isTop ? " 👑" : ""));
            userName.getStyleClass().add("bid-entry-user");
            Label timeLabel = new Label(TimeUtil.timeAgo(bid.getTimestamp()));
            timeLabel.getStyleClass().add("bid-entry-time");
            userInfo.getChildren().addAll(userName, timeLabel);
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label amtLabel = new Label(TimeUtil.formatCurrency(bid.getAmount()));
            amtLabel.getStyleClass().add("bid-entry-amount");
            entry.getChildren().addAll(userInfo, sp, amtLabel);
            histList.getChildren().add(entry);
        }
        return new VBox(histHeader, histList);
    }

    private VBox buildRightColumn(Auction a, Item item) {
        VBox rightCol = new VBox();
        rightCol.getStyleClass().add("auction-info-card");
        rightCol.setPrefWidth(400); rightCol.setMinWidth(400); rightCol.setMaxWidth(400);
        rightCol.setPadding(new Insets(24));
        rightCol.getChildren().addAll(
                buildCurrentBidDisplay(a), buildCountdown(a),
                buildBidArea(a), buildSellerRow(a), buildDivider(), buildListingDetails(a, item));
        return rightCol;
    }

    private VBox buildCurrentBidDisplay(Auction a) {
        VBox box = new VBox(); box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("current-bid-display"); box.setPadding(new Insets(20, 0, 20, 0));
        Label lbl = new Label("CURRENT BID"); lbl.getStyleClass().add("current-bid-label");
        Label amt = new Label(TimeUtil.formatCurrency(a.getCurrentHighestBid())); amt.getStyleClass().add("current-bid-amount");
        Label cnt = new Label(a.getBidCount() + " bid" + (a.getBidCount() != 1 ? "s" : "")); cnt.getStyleClass().add("bid-count-info");
        box.getChildren().addAll(lbl, amt, cnt);
        return box;
    }

    private VBox buildCountdown(Auction a) {
        VBox section = new VBox(); VBox.setMargin(section, new Insets(20, 0, 20, 0));
        Label trLabel = new Label("TIME REMAINING"); trLabel.getStyleClass().add("time-remaining-label");
        VBox.setMargin(trLabel, new Insets(0, 0, 8, 0));
        long[] parts = TimeUtil.getCountdownParts(a.getEndTime());
        HBox cdRow = new HBox(6);
        String[] labels = {"DAYS", "HRS", "MIN", "SEC"};
        for (int i = 0; i < 4; i++) {
            VBox unit = new VBox(); unit.getStyleClass().add("countdown-unit");
            unit.setAlignment(Pos.CENTER); unit.setPadding(new Insets(10, 4, 10, 4));
            HBox.setHgrow(unit, Priority.ALWAYS);
            Label val = new Label(String.format("%02d", parts[i])); val.getStyleClass().add("countdown-value");
            Label lbl = new Label(labels[i]); lbl.getStyleClass().add("countdown-label");
            unit.getChildren().addAll(val, lbl); cdRow.getChildren().add(unit);
        }
        section.getChildren().addAll(trLabel, cdRow);
        return section;
    }

    private VBox buildBidArea(Auction a) {
        VBox bidArea = new VBox();
        User currentUser = authService.getCurrentUser();
        if (a.isExpired() || a.getState() == AuctionState.FINISHED) {
            Label ended = new Label("This auction has ended.");
            ended.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-padding: 16 0;");
            ended.setAlignment(Pos.CENTER); ended.setMaxWidth(Double.MAX_VALUE);
            bidArea.getChildren().add(ended);
        } else if (currentUser == null) {
            Label prompt = new Label("Sign in to place a bid");
            prompt.setStyle("-fx-text-fill: #8fa8c0; -fx-font-size: 14px; -fx-padding: 0 0 16 0;");
            prompt.setAlignment(Pos.CENTER); prompt.setMaxWidth(Double.MAX_VALUE);
            Button signInBtn = new Button("Sign In to Bid");
            signInBtn.getStyleClass().add("btn-primary-lg"); signInBtn.setMaxWidth(Double.MAX_VALUE);
            signInBtn.setOnAction(e -> onShowSignIn.run()); // Lambda
            bidArea.getChildren().addAll(prompt, signInBtn);
        } else if (currentUser.getId().equals(a.getSellerId())) {
            Label seller = new Label("You are the seller of this item.");
            seller.setStyle("-fx-text-fill: #8fa8c0; -fx-padding: 16 0;");
            seller.setAlignment(Pos.CENTER); seller.setMaxWidth(Double.MAX_VALUE);
            bidArea.getChildren().add(seller);
        } else {
            double minBid = a.getCurrentHighestBid() + 1;
            TextField bidInput = new TextField();
            bidInput.getStyleClass().add("bid-input");
            bidInput.setPromptText(String.valueOf((int) minBid));
            HBox.setHgrow(bidInput, Priority.ALWAYS);
            Button bidBtn = new Button("🔨 Bid");
            bidBtn.getStyleClass().add("btn-primary");
            bidBtn.setOnAction(e -> onPlaceBid(bidInput)); // Lambda
            HBox row = new HBox(10, bidInput, bidBtn);
            VBox.setMargin(row, new Insets(0, 0, 10, 0));
            Label hint = new Label("Enter " + TimeUtil.formatCurrency(minBid) + " or more to bid");
            hint.getStyleClass().add("bid-hint"); VBox.setMargin(hint, new Insets(0, 0, 16, 0));
            bidArea.getChildren().addAll(row, hint);
        }
        return bidArea;
    }

    private HBox buildSellerRow(Auction a) {
        HBox row = new HBox(12); row.getStyleClass().add("seller-info");
        row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(16, 0, 0, 0));
        VBox.setMargin(row, new Insets(16, 0, 0, 0));
        StackPane avatar = new StackPane(); avatar.getStyleClass().add("seller-avatar");
        avatar.setMinSize(38, 38); avatar.setPrefSize(38, 38);
        String initials = a.getSellerName().length() >= 2
                ? ("" + a.getSellerName().charAt(0) + a.getSellerName().charAt(a.getSellerName().indexOf(' ') + 1)).toUpperCase()
                : a.getSellerName().substring(0, 1).toUpperCase();
        Label avText = new Label(initials); avText.getStyleClass().add("seller-avatar-text");
        avatar.getChildren().add(avText);
        VBox info = new VBox(2);
        Label name = new Label(a.getSellerName()); name.getStyleClass().add("seller-name");
        Label badge = new Label("⭐ Verified Seller"); badge.getStyleClass().add("seller-label");
        info.getChildren().addAll(name, badge);
        row.getChildren().addAll(avatar, info);
        return row;
    }

    private Region buildDivider() {
        Region d = new Region(); d.getStyleClass().add("divider");
        VBox.setMargin(d, new Insets(16, 0, 16, 0)); return d;
    }

    private VBox buildListingDetails(Auction a, Item item) {
        VBox details = new VBox(8);
        details.getChildren().addAll(
                detailRow("Starting bid", TimeUtil.formatCurrency(item.getStartPrice())),
                detailRow("Listed",       TimeUtil.formatDate(a.getStartTime())),
                detailRow("Ends",         TimeUtil.formatDate(a.getEndTime())));
        return details;
    }

    private HBox detailRow(String label, String value) {
        HBox row = new HBox();
        Label l = new Label(label); l.getStyleClass().add("detail-info-label");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value); v.getStyleClass().add("detail-info-value");
        row.getChildren().addAll(l, sp, v);
        return row;
    }
}
