package com.adjuge.controller;

import com.adjuge.model.*;
import com.adjuge.pattern.AuctionObserver;
import com.adjuge.pattern.DataStore;
import com.adjuge.pattern.ItemFactory;
import com.adjuge.service.*;
import com.adjuge.exception.*;
import com.adjuge.util.TimeUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Main controller for the BidVault application.
 *
 * This controller manages ALL views in the single-page FXML layout:
 * - Home view:      Browse auctions, search, filter by category
 * - Auth view:      Login and registration forms
 * - Auction detail: View item details, place bids, see bid history
 * - Dashboard:      User's bids and listings overview
 * - Create view:    List a new item for auction
 *
 * Implements AuctionObserver (Observer Pattern) to receive real-time bid updates.
 * Implements Initializable to set up the UI when the FXML is loaded.
 */
public class BiddingController implements Initializable, AuctionObserver {

    // ================================================================
    // FXML-INJECTED UI ELEMENTS
    // ================================================================

    // -- Views (ScrollPanes that are shown/hidden) --
    @FXML private ScrollPane viewHome;
    @FXML private ScrollPane viewAuth;
    @FXML private ScrollPane viewAuction;
    @FXML private ScrollPane viewDashboard;
    @FXML private ScrollPane viewCreate;

    // -- Header --
    @FXML private TextField searchField;
    @FXML private Button btnSignIn;
    @FXML private Label navUserLabel;

    // -- Hero stats --
    @FXML private Label statAuctions;
    @FXML private Label statUsers;
    @FXML private Label statBids;

    // -- Auction grid --
    @FXML private TilePane auctionGrid;

    // -- Auth forms --
    @FXML private VBox formLogin;
    @FXML private VBox formRegister;
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;
    @FXML private TextField regFirstName;
    @FXML private TextField regLastName;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regError;

    // -- Auction detail --
    @FXML private HBox auctionDetailContent;

    // -- Dashboard --
    @FXML private VBox dashboardContent;

    // -- Create form --
    @FXML private TextField createTitle;
    @FXML private TextArea createDesc;
    @FXML private ComboBox<String> createCategory;
    @FXML private ComboBox<String> createCondition;
    @FXML private TextField createStartPrice;
    @FXML private ComboBox<String> createDuration;
    @FXML private Label createError;

    // ================================================================
    // SERVICES (business logic layer)
    // ================================================================
    private final AuthService authService = new AuthService();
    private final AuctionService auctionService = new AuctionService();
    private final BidService bidService = new BidService();

    // ================================================================
    // STATE
    // ================================================================
    private String currentCategory = "All";
    private Auction currentAuction;       // Currently viewed auction detail
    private Timeline countdownTimeline;   // Timer that updates countdown every second

    // ================================================================
    // INITIALIZATION
    // ================================================================

    /**
     * Called automatically when the FXML is loaded.
     * Sets up the initial state of the application.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateStats();
        loadAuctionCards();
        startCountdownTimer();
    }

    // ================================================================
    // NAVIGATION — switching between views
    // ================================================================

    /** Show a specific view and hide all others */
    private void showView(ScrollPane view) {
        for (ScrollPane v : new ScrollPane[]{viewHome, viewAuth, viewAuction, viewDashboard, viewCreate}) {
            if (v != null) {
                v.setVisible(v == view);
                v.setManaged(v == view);
            }
        }
    }

    @FXML private void onBrowse() { showView(viewHome); }

    @FXML private void onSignIn() {
        if (authService.getCurrentUser() != null) { showView(viewDashboard); renderDashboard(); }
        else { showView(viewAuth); onTabSignIn(); }
    }

    @FXML private void onExploreAuctions() {
        showView(viewHome);
        // Scroll down to the auctions section
        if (auctionGrid != null && auctionGrid.getParent() != null) {
            viewHome.setVvalue(0.5);
        }
    }

    @FXML private void onStartSelling() {
        if (authService.isLoggedIn()) {
            showView(viewCreate);
        } else {
            showView(viewAuth);
            onTabSignIn();
        }
    }

    @FXML private void onBackToAuctions() {
        // Unsubscribe from current auction's observer
        if (currentAuction != null) {
            currentAuction.removeObserver(this);
            currentAuction = null;
        }
        showView(viewHome);
    }

    @FXML private void onBackFromCreate() { showView(viewHome); }

    // ================================================================
    // HOME VIEW — Stats, Cards, Search, Filters
    // ================================================================

    /** Update the hero section statistics */
    private void updateStats() {
        List<Auction> active = auctionService.getActiveAuctions();
        statAuctions.setText(String.valueOf(active.size()));
        statUsers.setText(String.valueOf(DataStore.getInstance().getAllUsers().size()));
        statBids.setText(auctionService.getTotalBidCount() + "+");
    }

    /** Populate the auction cards grid */
    private void loadAuctionCards() {
        auctionGrid.getChildren().clear();

        // Get auctions based on current search and category filter
        String query = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim() : "";
        Category catFilter = "All".equals(currentCategory) ? null
                : Category.fromDisplayName(currentCategory);

        List<Auction> auctions = auctionService.searchAuctions(
                query.isEmpty() ? null : query, catFilter);

        if (auctions.isEmpty()) {
            // Show "no results" message
            Label noResults = new Label("No auctions found");
            noResults.setStyle("-fx-text-fill: #8fa8c0; -fx-font-size: 16px;");
            noResults.setPadding(new Insets(64));
            auctionGrid.getChildren().add(noResults);
            return;
        }

        // Build a card for each auction
        // POLYMORPHISM: auction.getItem() returns Item, but the actual type
        // could be Electronics, Art, Vehicle, Fashion, etc.
        // Calling getCategoryLabel() and getItemSpecifics() gives different
        // results depending on the actual subtype.
        for (Auction auction : auctions) {
            VBox card = buildAuctionCard(auction);
            auctionGrid.getChildren().add(card);
        }
    }

    /**
     * Build a single auction card (matching the HTML card design).
     *
     * This method showcases POLYMORPHISM: we call auction.getItem().getCategoryLabel()
     * which returns different strings depending on whether the item is Electronics,
     * Art, Vehicle, Fashion, etc.
     */
    private VBox buildAuctionCard(Auction auction) {
        Item item = auction.getItem(); // POLYMORPHIC reference

        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> openAuctionDetail(auction.getId()));

        // -- Card image area --
        StackPane imageArea = new StackPane();
        imageArea.getStyleClass().add("card-image");
        imageArea.setMinHeight(200);
        imageArea.setPrefHeight(200);
        imageArea.setMaxHeight(200);

        // Try to load the image
        try {
            ImageView imgView = new ImageView(new Image(item.getImageUrl(), 420, 200, false, true, true));
            imgView.setFitWidth(420);
            imgView.setFitHeight(200);
            imgView.setPreserveRatio(false);
            imgView.setSmooth(true);
            imageArea.getChildren().add(imgView);
        } catch (Exception e) {
            // Fallback — just show the background color
        }

        // Condition badge (top-left)
        Label badge = new Label(item.getCondition());
        badge.getStyleClass().add("card-badge");
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.TOP_LEFT);
        badgeBox.setPadding(new Insets(12, 0, 0, 12));
        badgeBox.setMouseTransparent(true);
        StackPane.setAlignment(badgeBox, Pos.TOP_LEFT);

        // Timer badge (top-right)
        boolean urgent = TimeUtil.isEndingSoon(auction.getEndTime());
        Label timer = new Label("\u23F1 " + TimeUtil.shortTimer(auction.getEndTime()));
        timer.getStyleClass().add(urgent ? "card-timer-urgent" : "card-timer");
        HBox timerBox = new HBox(timer);
        timerBox.setAlignment(Pos.TOP_RIGHT);
        timerBox.setPadding(new Insets(12, 12, 0, 0));
        timerBox.setMouseTransparent(true);
        StackPane.setAlignment(timerBox, Pos.TOP_RIGHT);

        imageArea.getChildren().addAll(badgeBox, timerBox);

        // -- Card body --
        VBox body = new VBox();
        body.getStyleClass().add("card-body");
        body.setPadding(new Insets(20));

        // POLYMORPHISM: getCategoryLabel() returns different values per Item subtype
        Label category = new Label(item.getCategory().getDisplayName());
        category.getStyleClass().add("card-category");

        Label title = new Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);

        Label desc = new Label(item.getDescription().length() > 100
                ? item.getDescription().substring(0, 100) + "..."
                : item.getDescription());
        desc.getStyleClass().add("card-description");
        desc.setWrapText(true);
        VBox.setMargin(desc, new Insets(0, 0, 16, 0));

        // Footer with bid info
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

        Label bidCount = new Label(auction.getBidCount() + " bid" + (auction.getBidCount() != 1 ? "s" : ""));
        bidCount.getStyleClass().add("bid-count");

        footer.getChildren().addAll(bidInfo, spacer, bidCount);
        body.getChildren().addAll(category, title, desc, footer);
        card.getChildren().addAll(imageArea, body);

        return card;
    }

    /** Called when user types in the search bar */
    @FXML private void onSearchChanged() {
        loadAuctionCards();
    }

    /** Called when user clicks a category filter chip */
    @FXML private void onFilterCategory(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        currentCategory = btn.getText();

        // Update active state visually on all filter chips
        if (btn.getParent() instanceof HBox filterBar) {
            for (javafx.scene.Node child : filterBar.getChildren()) {
                if (child instanceof Button filterBtn) {
                    filterBtn.getStyleClass().removeAll("filter-chip-active", "filter-chip");
                    filterBtn.getStyleClass().add(
                            filterBtn.getText().equals(currentCategory) ? "filter-chip-active" : "filter-chip");
                }
            }
        }
        loadAuctionCards();
    }

    // ================================================================
    // AUTH VIEW — Login and Registration
    // ================================================================

    /** Switch to the Sign In form */
    @FXML private void onTabSignIn() {
        formLogin.setVisible(true);
        formLogin.setManaged(true);
        formRegister.setVisible(false);
        formRegister.setManaged(false);
        loginError.setText("");
    }

    /** Switch to the Create Account form */
    @FXML private void onTabCreateAccount() {
        formRegister.setVisible(true);
        formRegister.setManaged(true);
        formLogin.setVisible(false);
        formLogin.setManaged(false);
        regError.setText("");
    }

    /** Handle login form submission */
    @FXML private void onLogin() {
        try {
            User user = authService.login(
                    loginEmail.getText().trim(),
                    loginPassword.getText());
            loginError.setText("");
            onLoginSuccess(user);
        } catch (AuthenticationException e) {
            loginError.setText(e.getMessage());
        }
    }

    /** Handle registration form submission */
    @FXML private void onRegister() {
        try {
            User user = authService.register(
                    regFirstName.getText().trim(),
                    regLastName.getText().trim(),
                    regEmail.getText().trim(),
                    regPassword.getText(),
                    regConfirm.getText());
            regError.setText("");
            onLoginSuccess(user);
        } catch (AuthenticationException e) {
            regError.setText(e.getMessage());
        }
    }

    /** After successful login/register, go to home and update nav */
    private void onLoginSuccess(User user) {
        // Update nav buttons to show user is logged in
        navUserLabel.setText(user.getFirstName());
        navUserLabel.setVisible(true);
        navUserLabel.setManaged(true);
        btnSignIn.setText("Dashboard");
        btnSignIn.setOnAction(e -> {
            showView(viewDashboard);
            renderDashboard();
        });
        showView(viewHome);
        updateStats();
    }

    // ================================================================
    // AUCTION DETAIL VIEW
    // ================================================================

    /** Open the detail view for a specific auction */
    private void openAuctionDetail(String auctionId) {
        Auction auction = auctionService.getAuction(auctionId);
        if (auction == null) return;

        // Unsubscribe from previous auction
        if (currentAuction != null) {
            currentAuction.removeObserver(this);
        }

        currentAuction = auction;
        // Subscribe to this auction for real-time updates (Observer Pattern)
        currentAuction.addObserver(this);

        renderAuctionDetail();
        showView(viewAuction);
    }

    /** Render the full auction detail page */
    private void renderAuctionDetail() {
        if (currentAuction == null || auctionDetailContent == null) return;
        auctionDetailContent.getChildren().clear();

        Auction a = currentAuction;
        Item item = a.getItem(); // POLYMORPHIC — could be any Item subtype

        // ---- LEFT COLUMN: Image + Description + Bid History ----
        VBox leftCol = new VBox();
        leftCol.setSpacing(0);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // Main image
        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("auction-image-main");
        imagePane.setMinHeight(450);
        imagePane.setPrefHeight(450);
        try {
            ImageView img = new ImageView(new Image(item.getImageUrl(), 900, 450, false, true, true));
            img.setFitWidth(900);
            img.setFitHeight(450);
            img.setPreserveRatio(false);
            imagePane.getChildren().add(img);
        } catch (Exception e) { /* fallback background */ }

        // Meta tags — POLYMORPHISM: getCategoryLabel() differs per subtype
        HBox metaTags = new HBox(12);
        metaTags.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(metaTags, new Insets(16, 0, 16, 0));
        Label catTag = new Label(item.getCategory().getDisplayName());
        catTag.getStyleClass().add("meta-tag-gold");
        Label condTag = new Label("Condition: " + item.getCondition());
        condTag.getStyleClass().add("meta-tag");
        // POLYMORPHISM: getItemSpecifics() shows type-specific details
        Label specTag = new Label(item.getItemSpecifics());
        specTag.getStyleClass().add("meta-tag");
        metaTags.getChildren().addAll(catTag, condTag, specTag);

        // Title
        Label titleLabel = new Label(item.getName());
        titleLabel.getStyleClass().add("detail-title");
        titleLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(item.getDescription());
        descLabel.getStyleClass().add("detail-description");
        descLabel.setWrapText(true);
        VBox.setMargin(descLabel, new Insets(16, 0, 32, 0));

        // Bid history header
        Label histTitle = new Label("Bid History");
        histTitle.getStyleClass().add("bid-history-title");
        Label histCount = new Label("(" + a.getBidCount() + " bid" + (a.getBidCount() != 1 ? "s" : "") + ")");
        histCount.getStyleClass().add("bid-history-count");
        HBox histHeader = new HBox(8, histTitle, histCount);
        histHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(histHeader, new Insets(0, 0, 16, 0));

        // Bid history entries
        VBox histList = new VBox(6);
        histList.getStyleClass().add("bid-history-list");
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
            Label userName = new Label(bid.getBidderName() + (isTop ? " \uD83D\uDC51" : ""));
            userName.getStyleClass().add("bid-entry-user");
            Label timeLabel = new Label(TimeUtil.timeAgo(bid.getTimestamp()));
            timeLabel.getStyleClass().add("bid-entry-time");
            userInfo.getChildren().addAll(userName, timeLabel);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label amtLabel = new Label(TimeUtil.formatCurrency(bid.getAmount()));
            amtLabel.getStyleClass().add("bid-entry-amount");

            entry.getChildren().addAll(userInfo, sp, amtLabel);
            histList.getChildren().add(entry);
        }

        leftCol.getChildren().addAll(imagePane, metaTags, titleLabel, descLabel, histHeader, histList);

        // ---- RIGHT COLUMN: Info Card ----
        VBox rightCol = new VBox();
        rightCol.getStyleClass().add("auction-info-card");
        rightCol.setPrefWidth(400);
        rightCol.setMinWidth(400);
        rightCol.setMaxWidth(400);
        rightCol.setPadding(new Insets(24));

        // Current bid display
        VBox bidDisplay = new VBox();
        bidDisplay.setAlignment(Pos.CENTER);
        bidDisplay.getStyleClass().add("current-bid-display");
        bidDisplay.setPadding(new Insets(20, 0, 20, 0));
        Label cbLabel = new Label("CURRENT BID");
        cbLabel.getStyleClass().add("current-bid-label");
        Label cbAmount = new Label(TimeUtil.formatCurrency(a.getCurrentHighestBid()));
        cbAmount.getStyleClass().add("current-bid-amount");
        Label cbCount = new Label(a.getBidCount() + " bid" + (a.getBidCount() != 1 ? "s" : ""));
        cbCount.getStyleClass().add("bid-count-info");
        bidDisplay.getChildren().addAll(cbLabel, cbAmount, cbCount);

        // Countdown
        VBox countdownSection = new VBox();
        VBox.setMargin(countdownSection, new Insets(20, 0, 20, 0));
        Label trLabel = new Label("TIME REMAINING");
        trLabel.getStyleClass().add("time-remaining-label");
        VBox.setMargin(trLabel, new Insets(0, 0, 8, 0));

        long[] parts = TimeUtil.getCountdownParts(a.getEndTime());
        HBox cdRow = new HBox(6);
        String[] labels = {"DAYS", "HRS", "MIN", "SEC"};
        for (int i = 0; i < 4; i++) {
            VBox unit = new VBox();
            unit.getStyleClass().add("countdown-unit");
            unit.setAlignment(Pos.CENTER);
            unit.setPadding(new Insets(10, 4, 10, 4));
            HBox.setHgrow(unit, Priority.ALWAYS);
            Label val = new Label(String.format("%02d", parts[i]));
            val.getStyleClass().add("countdown-value");
            Label lbl = new Label(labels[i]);
            lbl.getStyleClass().add("countdown-label");
            unit.getChildren().addAll(val, lbl);
            cdRow.getChildren().add(unit);
        }
        countdownSection.getChildren().addAll(trLabel, cdRow);

        // Bid input area
        VBox bidArea = new VBox();
        User currentUser = authService.getCurrentUser();
        if (a.isExpired() || a.getState() == AuctionState.FINISHED) {
            Label ended = new Label("This auction has ended.");
            ended.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: 600; -fx-padding: 16 0;");
            ended.setAlignment(Pos.CENTER);
            ended.setMaxWidth(Double.MAX_VALUE);
            bidArea.getChildren().add(ended);
        } else if (currentUser == null) {
            Label signInPrompt = new Label("Sign in to place a bid");
            signInPrompt.setStyle("-fx-text-fill: #8fa8c0; -fx-font-size: 14px; -fx-padding: 0 0 16 0;");
            signInPrompt.setAlignment(Pos.CENTER);
            signInPrompt.setMaxWidth(Double.MAX_VALUE);
            Button signInBtn = new Button("Sign In to Bid");
            signInBtn.getStyleClass().add("btn-primary-lg");
            signInBtn.setMaxWidth(Double.MAX_VALUE);
            signInBtn.setOnAction(e -> { showView(viewAuth); onTabSignIn(); });
            bidArea.getChildren().addAll(signInPrompt, signInBtn);
        } else if (currentUser.getId().equals(a.getSellerId())) {
            Label seller = new Label("You are the seller of this item.");
            seller.setStyle("-fx-text-fill: #8fa8c0; -fx-padding: 16 0;");
            seller.setAlignment(Pos.CENTER);
            seller.setMaxWidth(Double.MAX_VALUE);
            bidArea.getChildren().add(seller);
        } else {
            double minBid = a.getCurrentHighestBid() + 1;
            HBox bidInputRow = new HBox(10);
            VBox.setMargin(bidInputRow, new Insets(0, 0, 10, 0));
            TextField bidInput = new TextField();
            bidInput.getStyleClass().add("bid-input");
            bidInput.setPromptText(String.valueOf((int) minBid));
            HBox.setHgrow(bidInput, Priority.ALWAYS);
            Button bidBtn = new Button("\uD83D\uDD28 Bid");
            bidBtn.getStyleClass().add("btn-primary");
            bidBtn.setOnAction(e -> onPlaceBid(bidInput));
            bidInputRow.getChildren().addAll(bidInput, bidBtn);

            Label hint = new Label("Enter " + TimeUtil.formatCurrency(minBid) + " or more to bid");
            hint.getStyleClass().add("bid-hint");
            VBox.setMargin(hint, new Insets(0, 0, 16, 0));

            bidArea.getChildren().addAll(bidInputRow, hint);
        }

        // Seller info
        HBox sellerRow = new HBox(12);
        sellerRow.getStyleClass().add("seller-info");
        sellerRow.setAlignment(Pos.CENTER_LEFT);
        sellerRow.setPadding(new Insets(16, 0, 0, 0));
        VBox.setMargin(sellerRow, new Insets(16, 0, 0, 0));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("seller-avatar");
        avatar.setMinSize(38, 38);
        avatar.setPrefSize(38, 38);
        String initials = a.getSellerName().length() >= 2
                ? ("" + a.getSellerName().charAt(0) + a.getSellerName().charAt(a.getSellerName().indexOf(' ') + 1)).toUpperCase()
                : a.getSellerName().substring(0, 1).toUpperCase();
        Label avText = new Label(initials);
        avText.getStyleClass().add("seller-avatar-text");
        avatar.getChildren().add(avText);

        VBox sellerInfo = new VBox(2);
        Label sellerName = new Label(a.getSellerName());
        sellerName.getStyleClass().add("seller-name");
        Label sellerLabel = new Label("\u2B50 Verified Seller");
        sellerLabel.getStyleClass().add("seller-label");
        sellerInfo.getChildren().addAll(sellerName, sellerLabel);
        sellerRow.getChildren().addAll(avatar, sellerInfo);

        // Divider
        Region divider = new Region();
        divider.getStyleClass().add("divider");
        VBox.setMargin(divider, new Insets(16, 0, 16, 0));

        // Listing details
        VBox details = new VBox(8);
        details.getChildren().addAll(
                detailRow("Starting bid", TimeUtil.formatCurrency(item.getStartPrice())),
                detailRow("Listed", TimeUtil.formatDate(a.getStartTime())),
                detailRow("Ends", TimeUtil.formatDate(a.getEndTime()))
        );

        rightCol.getChildren().addAll(bidDisplay, countdownSection, bidArea, sellerRow, divider, details);
        auctionDetailContent.getChildren().addAll(leftCol, rightCol);
        auctionDetailContent.setSpacing(32);
    }

    /** Create a detail row (label — value) for the info card */
    private HBox detailRow(String label, String value) {
        HBox row = new HBox();
        Label l = new Label(label);
        l.getStyleClass().add("detail-info-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.getStyleClass().add("detail-info-value");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    /** Handle placing a bid */
    private void onPlaceBid(TextField bidInput) {
        User user = authService.getCurrentUser();
        if (user == null || currentAuction == null) return;

        try {
            double amount = Double.parseDouble(bidInput.getText().trim());
            // BidService.placeBid is SYNCHRONIZED for thread safety
            bidService.placeBid(currentAuction, user, amount);
            // Observer pattern: onNewBid() will be called, which re-renders
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid number.");
        } catch (InvalidBidException e) {
            showAlert("Invalid Bid", e.getMessage());
        } catch (AuctionClosedException e) {
            showAlert("Auction Closed", e.getMessage());
        }
    }

    // ================================================================
    // OBSERVER PATTERN — Real-time updates
    // ================================================================

    /**
     * Called when a new bid is placed on an auction we're observing.
     * Updates the UI on the JavaFX Application Thread.
     */
    @Override
    public void onNewBid(Auction auction, BidTransaction bid) {
        Platform.runLater(() -> {
            if (currentAuction != null && currentAuction.getId().equals(auction.getId())) {
                renderAuctionDetail(); // Re-render with updated data
            }
            updateStats();
            loadAuctionCards();
        });
    }

    /**
     * Called when an auction's state changes (e.g., RUNNING → FINISHED).
     * Updates the UI accordingly.
     */
    @Override
    public void onStateChange(Auction auction, AuctionState oldState, AuctionState newState) {
        Platform.runLater(() -> {
            if (currentAuction != null && currentAuction.getId().equals(auction.getId())) {
                renderAuctionDetail();
            }
            loadAuctionCards();
        });
    }

    // ================================================================
    // DASHBOARD VIEW
    // ================================================================

    /** Render the user dashboard with their bids and listings */
    private void renderDashboard() {
        User user = authService.getCurrentUser();
        if (user == null) { showView(viewAuth); onTabSignIn(); return; }
        if (dashboardContent == null) return;
        dashboardContent.getChildren().clear();

        List<Auction> myBidAuctions = auctionService.getAuctionsWithUserBids(user.getId());
        List<Auction> myListings    = auctionService.getAuctionsBySeller(user.getId());
        long activeBids = myBidAuctions.stream().filter(a -> !a.isExpired()).count();

        // Sync listing count from real data (Seller.totalListings isn't auto-updated)
        if (user instanceof Seller) ((Seller) user).setTotalListings(myListings.size());
        List<Auction> wonAuctions = myBidAuctions.stream()
                .filter(a -> a.isExpired() && user.getId().equals(a.getHighestBidderId()))
                .collect(Collectors.toList());
        double totalSpent = wonAuctions.stream().mapToDouble(Auction::getCurrentHighestBid).sum();

        // ── Profile card ──────────────────────────────────────────────────
        String initials = user.getFirstName().substring(0, 1).toUpperCase()
                        + user.getLastName().substring(0, 1).toUpperCase();
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("profile-avatar");
        avatar.setMinSize(64, 64); avatar.setMaxSize(64, 64);
        Label avatarText = new Label(initials);
        avatarText.getStyleClass().add("profile-avatar-text");
        avatar.getChildren().add(avatarText);

        Label nameLabel   = new Label(user.getFirstName() + " " + user.getLastName());
        nameLabel.getStyleClass().add("profile-name");
        Label emailLabel  = new Label(user.getEmail());
        emailLabel.getStyleClass().add("profile-email");
        Label memberLabel = new Label("Member since " + TimeUtil.formatDate(user.getCreatedAt()));
        memberLabel.getStyleClass().add("profile-meta");
        Label roleLabel = new Label(user.getRole().toString());
        roleLabel.getStyleClass().add("role-badge");
        HBox badgeRow = new HBox(8, roleLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
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

        // ── Stats row ─────────────────────────────────────────────────────
        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
            statCard("ACTIVE BIDS",   String.valueOf(activeBids),        "Currently bidding",  false),
            statCard("AUCTIONS WON",  String.valueOf(wonAuctions.size()), "Items secured",     true),
            statCard("TOTAL SPENT",   TimeUtil.formatCurrency(totalSpent), "On won items",     false),
            statCard("MY LISTINGS",   String.valueOf(myListings.size()),  "Items for sale",    false)
        );
        for (javafx.scene.Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        VBox.setMargin(statsRow, new Insets(0, 0, 20, 0));

        // ── Two-column activity ───────────────────────────────────────────
        VBox leftCol  = buildBidActivitySection(user, myBidAuctions);
        VBox rightCol = buildListingsSection(myListings);
        HBox.setHgrow(leftCol,  Priority.ALWAYS);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        HBox columns = new HBox(16, leftCol, rightCol);

        dashboardContent.getChildren().addAll(profileCard, statsRow, columns);
    }

    /** Stat card with optional gold accent */
    private VBox statCard(String label, String value, String sub, boolean gold) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(20));
        Label l = new Label(label);
        l.getStyleClass().add("stat-card-label");
        Label v = new Label(value);
        v.getStyleClass().add(gold ? "stat-card-value-gold" : "stat-card-value");
        Label s = new Label(sub);
        s.getStyleClass().add("stat-card-sub");
        card.getChildren().addAll(l, v, s);
        return card;
    }

    /** Bid activity section — shows all auctions the user has bid on */
    private VBox buildBidActivitySection(User user, List<Auction> bidAuctions) {
        VBox section = new VBox(12);
        section.getStyleClass().add("dash-section-card");

        Label title = new Label("Bid Activity");
        title.getStyleClass().add("dash-section-title");
        section.getChildren().add(title);

        if (bidAuctions.isEmpty()) {
            Label empty = new Label("You haven't placed any bids yet.");
            empty.getStyleClass().add("dash-empty-state");
            section.getChildren().add(empty);
            return section;
        }

        for (Auction a : bidAuctions) {
            // Find this user's highest bid on this auction
            double myBid = a.getBids().stream()
                    .filter(b -> user.getId().equals(b.getBidderId()))
                    .mapToDouble(b -> b.getAmount())
                    .max().orElse(0);

            boolean isWinning = user.getId().equals(a.getHighestBidderId());
            boolean isExpired = a.isExpired();

            String statusText;
            String statusStyle;
            if (isExpired && isWinning)      { statusText = "WON";    statusStyle = "status-won"; }
            else if (isExpired)              { statusText = "LOST";   statusStyle = "status-closed"; }
            else if (isWinning)              { statusText = "WINNING"; statusStyle = "status-winning"; }
            else                             { statusText = "OUTBID"; statusStyle = "status-outbid"; }

            Label itemTitle = new Label(truncate(a.getItem().getName(), 38));
            itemTitle.getStyleClass().add("bid-row-title");

            Label myBidLabel = new Label("Your bid: " + TimeUtil.formatCurrency(myBid));
            myBidLabel.getStyleClass().add("bid-row-meta");

            Label currentLabel = new Label("Current: " + TimeUtil.formatCurrency(a.getCurrentHighestBid()));
            currentLabel.getStyleClass().add("bid-row-current");

            Label statusBadge = new Label(statusText);
            statusBadge.getStyleClass().addAll("status-badge", statusStyle);

            Label timeLabel = new Label(isExpired ? "Ended" : TimeUtil.shortTimer(a.getEndTime()));
            timeLabel.getStyleClass().add("bid-row-time");

            Region rowGap = new Region(); HBox.setHgrow(rowGap, Priority.ALWAYS);
            HBox topRow = new HBox(8, itemTitle, rowGap, statusBadge);
            topRow.setAlignment(Pos.CENTER_LEFT);
            HBox bottomRow = new HBox(16, myBidLabel, currentLabel, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, timeLabel);
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            VBox row = new VBox(4, topRow, bottomRow);
            row.getStyleClass().add("bid-row");
            section.getChildren().add(row);
        }
        return section;
    }

    /** Listings section — shows auctions created by the user */
    private VBox buildListingsSection(List<Auction> listings) {
        VBox section = new VBox(12);
        section.getStyleClass().add("dash-section-card");

        Label title = new Label("My Listings");
        title.getStyleClass().add("dash-section-title");
        section.getChildren().add(title);

        if (listings.isEmpty()) {
            Label empty = new Label("No listings yet. Use \"List an Item\" to get started.");
            empty.getStyleClass().add("dash-empty-state");
            empty.setWrapText(true);
            section.getChildren().add(empty);
            return section;
        }

        for (Auction a : listings) {
            boolean isExpired = a.isExpired();
            String statusText  = isExpired ? "CLOSED" : "LIVE";
            String statusStyle = isExpired ? "status-closed" : "status-winning";

            Label itemTitle = new Label(truncate(a.getItem().getName(), 34));
            itemTitle.getStyleClass().add("bid-row-title");

            Label statusBadge = new Label(statusText);
            statusBadge.getStyleClass().addAll("status-badge", statusStyle);

            Region rowGap = new Region(); HBox.setHgrow(rowGap, Priority.ALWAYS);
            HBox topRow = new HBox(8, itemTitle, rowGap, statusBadge);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label bidsLabel  = new Label(a.getBidCount() + " bids");
            bidsLabel.getStyleClass().add("bid-row-meta");
            Label priceLabel = new Label("Top: " + TimeUtil.formatCurrency(a.getCurrentHighestBid()));
            priceLabel.getStyleClass().add("bid-row-current");
            Label timeLabel  = new Label(isExpired ? "Ended" : TimeUtil.shortTimer(a.getEndTime()));
            timeLabel.getStyleClass().add("bid-row-time");

            HBox bottomRow = new HBox(16, bidsLabel, priceLabel, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, timeLabel);
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            VBox row = new VBox(4, topRow, bottomRow);
            row.getStyleClass().add("bid-row");
            section.getChildren().add(row);
        }
        return section;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ================================================================
    // CREATE AUCTION VIEW
    // ================================================================

    /** Handle creating a new auction listing */
    @FXML private void onCreateAuction() {
        User user = authService.getCurrentUser();
        if (user == null) {
            showView(viewAuth);
            return;
        }

        if (!(user instanceof Sellable)) {
            createError.setText("Your account type (" + user.getRole() + ") cannot create listings.");
            return;
        }

        String title = createTitle.getText().trim();
        String desc = createDesc.getText().trim();
        String catStr = createCategory.getValue();
        String condition = createCondition.getValue();
        String priceStr = createStartPrice.getText().trim();
        String durStr = createDuration.getValue();

        if (title.isEmpty() || desc.isEmpty() || catStr == null || priceStr.isEmpty()) {
            createError.setText("Please fill in all required fields.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 1) {
                createError.setText("Starting bid must be at least \\$1.");
                return;
            }
        } catch (NumberFormatException e) {
            createError.setText("Invalid price format.");
            return;
        }

        int days = Integer.parseInt(durStr.replaceAll("[^0-9]", ""));

        // FACTORY METHOD pattern: creates the correct Item subtype based on category
        Category category = Category.fromDisplayName(catStr);
        Item item = ItemFactory.createItem(category, title, desc, price,
                "https://picsum.photos/seed/" + System.currentTimeMillis() + "/800/600",
                condition != null ? condition : "Good");
        ValidatorType validatorType = ValidatorType.STANDARD;//Viết logic chọn Validator
        // Create the auction
        Auction auction = auctionService.createAuction(item, user.getId(),
                user.getFirstName() + " " + user.getLastName().charAt(0) + ".", days, validatorType);

        createError.setText("");
        createTitle.clear();
        createDesc.clear();
        createStartPrice.clear();

        // Navigate to the new auction
        updateStats();
        openAuctionDetail(auction.getId());
    }

    // ================================================================
    // COUNTDOWN TIMER — updates every second
    // ================================================================

    /** Start a timeline that refreshes card timers every second */
    private void startCountdownTimer() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Update card timers if on home view
            if (viewHome != null && viewHome.isVisible()) {
                // Lightweight update — just refresh cards
                // (In production, you'd only update timer labels, not rebuild cards)
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ================================================================
    // UTILITY METHODS
    // ================================================================

    /** Show a simple alert dialog */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
