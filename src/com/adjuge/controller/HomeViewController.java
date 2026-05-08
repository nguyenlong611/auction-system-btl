package com.adjuge.controller;

import com.adjuge.model.*;
import com.adjuge.pattern.DataStore;
import com.adjuge.service.AuctionService;
import com.adjuge.util.TimeUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * Sub-controller responsible for the Home view.
 *
 * Responsibilities (SRP):
 *  - Displaying live auction statistics (hero section)
 *  - Building and populating the auction card grid
 *  - Handling search input changes and category filter clicks
 *
 * This class does NOT own @FXML fields. The parent BiddingController
 * injects the required UI nodes via the constructor.
 */
public class HomeViewController {

    // ── Injected UI nodes ─────────────────────────────────────────────
    private final Label     statAuctions;
    private final Label     statUsers;
    private final Label     statBids;
    private final TilePane  auctionGrid;
    private final TextField searchField;
    private final ScrollPane viewHome;

    // ── Services ──────────────────────────────────────────────────────
    private final AuctionService auctionService;

    // ── Callbacks ─────────────────────────────────────────────────────
    private final Consumer<String> onOpenDetail;

    // ── State ─────────────────────────────────────────────────────────
    private String currentCategory = "All";

    // ─────────────────────────────────────────────────────────────────
    public HomeViewController(
            Label statAuctions, Label statUsers, Label statBids,
            TilePane auctionGrid, TextField searchField, ScrollPane viewHome,
            AuctionService auctionService,
            Consumer<String> onOpenDetail) {
        this.statAuctions   = statAuctions;
        this.statUsers      = statUsers;
        this.statBids       = statBids;
        this.auctionGrid    = auctionGrid;
        this.searchField    = searchField;
        this.viewHome       = viewHome;
        this.auctionService = auctionService;
        this.onOpenDetail   = onOpenDetail;
    }

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────

    /** Refresh the three hero-section statistics labels */
    public void updateStats() {
        List<Auction> active = auctionService.getActiveAuctions();
        statAuctions.setText(String.valueOf(active.size()));
        statUsers.setText(String.valueOf(DataStore.getInstance().getAllUsers().size()));
        statBids.setText(auctionService.getTotalBidCount() + "+");
    }

    /** Rebuild the auction card grid using current search / category filters */
    public void loadAuctionCards() {
        auctionGrid.getChildren().clear();

        String query = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim() : "";
        Category catFilter = "All".equals(currentCategory) ? null
                : Category.fromDisplayName(currentCategory);

        List<Auction> auctions = auctionService.searchAuctions(
                query.isEmpty() ? null : query, catFilter);

        if (auctions.isEmpty()) {
            Label noResults = new Label("No auctions found");
            noResults.setStyle("-fx-text-fill: #8fa8c0; -fx-font-size: 16px;");
            noResults.setPadding(new Insets(64));
            auctionGrid.getChildren().add(noResults);
            return;
        }

        // POLYMORPHISM: auction.getItem() may be Electronics, Art, Vehicle, etc.
        for (Auction auction : auctions) {
            auctionGrid.getChildren().add(buildAuctionCard(auction));
        }
    }

    /** Called when the user types in the search bar */
    public void onSearchChanged() { loadAuctionCards(); }

    /** Called when a category filter chip is clicked */
    public void onFilterCategory(Button btn, String categoryText) {
        currentCategory = categoryText;
        if (btn.getParent() instanceof HBox filterBar) {
            for (javafx.scene.Node child : filterBar.getChildren()) {
                if (child instanceof Button filterBtn) {
                    filterBtn.getStyleClass().removeAll("filter-chip-active", "filter-chip");
                    filterBtn.getStyleClass().add(
                            filterBtn.getText().equals(currentCategory)
                                    ? "filter-chip-active" : "filter-chip");
                }
            }
        }
        loadAuctionCards();
    }

    /** Scroll the home view down to the auction grid */
    public void scrollToAuctions() {
        if (viewHome != null) viewHome.setVvalue(0.5);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE — card building
    // ─────────────────────────────────────────────────────────────────

    private VBox buildAuctionCard(Auction auction) {
        Item item = auction.getItem(); // POLYMORPHIC reference

        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setCursor(javafx.scene.Cursor.HAND);
        // Lambda expression for event handling
        card.setOnMouseClicked(e -> onOpenDetail.accept(auction.getId()));

        StackPane imageArea = new StackPane();
        imageArea.getStyleClass().add("card-image");
        imageArea.setMinHeight(200);
        imageArea.setPrefHeight(200);
        imageArea.setMaxHeight(200);

        try {
            // Exception handling: image URL may be invalid
            ImageView imgView = new ImageView(
                    new Image(item.getImageUrl(), 420, 200, false, true, true));
            imgView.setFitWidth(420);
            imgView.setFitHeight(200);
            imgView.setPreserveRatio(false);
            imgView.setSmooth(true);
            imageArea.getChildren().add(imgView);
        } catch (Exception e) { /* fallback CSS background */ }

        Label badge = new Label(item.getCondition());
        badge.getStyleClass().add("card-badge");
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.TOP_LEFT);
        badgeBox.setPadding(new Insets(12, 0, 0, 12));
        badgeBox.setMouseTransparent(true);
        StackPane.setAlignment(badgeBox, Pos.TOP_LEFT);

        boolean urgent = TimeUtil.isEndingSoon(auction.getEndTime());
        Label timer = new Label("⏱ " + TimeUtil.shortTimer(auction.getEndTime()));
        timer.getStyleClass().add(urgent ? "card-timer-urgent" : "card-timer");
        HBox timerBox = new HBox(timer);
        timerBox.setAlignment(Pos.TOP_RIGHT);
        timerBox.setPadding(new Insets(12, 12, 0, 0));
        timerBox.setMouseTransparent(true);
        StackPane.setAlignment(timerBox, Pos.TOP_RIGHT);

        imageArea.getChildren().addAll(badgeBox, timerBox);

        VBox body = new VBox();
        body.getStyleClass().add("card-body");
        body.setPadding(new Insets(20));

        // The category display name comes from the Category enum stored on Item
        Label category = new Label(item.getCategory().getDisplayName());
        category.getStyleClass().add("card-category");

        Label title = new Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label desc = new Label(item.getDescription().length() > 100
                ? item.getDescription().substring(0, 100) + "…"
                : item.getDescription());
        desc.getStyleClass().add("card-description");
        desc.setWrapText(true);
        VBox.setMargin(desc, new Insets(0, 0, 16, 0));

        HBox footer = new HBox();
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.BOTTOM_LEFT);
        footer.setPadding(new Insets(16, 0, 0, 0));

        VBox bidInfo = new VBox();
        Label bidLabel = new Label("CURRENT BID");
        bidLabel.getStyleClass().add("bid-label");
        Label bidAmount = new Label(TimeUtil.formatCurrency(auction.getCurrentHighestBid()));
        bidAmount.getStyleClass().add("bid-amount");
        bidInfo.getChildren().addAll(bidLabel, bidAmount);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label bidCount = new Label(auction.getBidCount()
                + " bid" + (auction.getBidCount() != 1 ? "s" : ""));
        bidCount.getStyleClass().add("bid-count");

        footer.getChildren().addAll(bidInfo, spacer, bidCount);
        body.getChildren().addAll(category, title, desc, footer);
        card.getChildren().addAll(imageArea, body);
        return card;
    }
}
