package com.adjuge.controller;

import com.adjuge.model.*;
import com.adjuge.pattern.AuctionObserver;
import com.adjuge.service.*;
import com.adjuge.util.TimeUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the Adjugé! application — acts as the top-level "hub".
 *
 * After refactoring (SRP), this class is now responsible for only:
 *  1. Declaring and injecting all @FXML fields (required by JavaFX loader)
 *  2. Initialising the five sub-controllers and wiring their callbacks
 *  3. Navigation between the five views (showView)
 *  4. Implementing AuctionObserver (Observer Pattern) for real-time updates
 */
public class BiddingController implements Initializable, AuctionObserver {

    // ================================================================
    // FXML-INJECTED UI ELEMENTS
    // ================================================================

    @FXML private ScrollPane viewHome, viewAuth, viewAuction, viewDashboard, viewCreate;
    @FXML private TextField searchField;
    @FXML private Button btnSignIn;
    @FXML private Label navUserLabel, statAuctions, statUsers, statBids;
    @FXML private TilePane auctionGrid;

    // Auth forms
    @FXML private VBox formLogin, formRegister;
    @FXML private TextField loginEmail, regFirstName, regLastName, regEmail;
    @FXML private PasswordField loginPassword, regPassword, regConfirm;
    @FXML private Label loginError, regError;

    // View contents
    @FXML private HBox auctionDetailContent;
    @FXML private VBox dashboardContent;

    // Create form
    @FXML private TextField createTitle, createStartPrice;
    @FXML private TextArea createDesc;
    @FXML private ComboBox<String> createCategory, createCondition, createDuration;
    @FXML private Label createError;

    // ================================================================
    // SERVICES
    // ================================================================
    private final AuthService authService = new AuthService();
    private final AuctionService auctionService = new AuctionService();
    private final BidService bidService = new BidService();

    // ================================================================
    // SUB-CONTROLLERS (SRP)
    // ================================================================
    private HomeViewController homeVC;
    private AuthViewController authVC;
    private AuctionDetailViewController detailVC;
    private DashboardViewController dashboardVC;
    private CreateAuctionViewController createVC;

    private Auction currentAuction;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Wire up sub-controllers with their respective UI nodes and callbacks
        homeVC = new HomeViewController(statAuctions, statUsers, statBids, auctionGrid, searchField, viewHome, auctionService, this::openAuctionDetail);
        
        authVC = new AuthViewController(formLogin, formRegister, loginEmail, loginPassword, loginError, 
                                        regFirstName, regLastName, regEmail, regPassword, regConfirm, regError, 
                                        authService, this::onLoginSuccess);

        detailVC = new AuctionDetailViewController(auctionDetailContent, authService, bidService, 
                                                   () -> { showView(viewAuth); authVC.showSignInTab(); }, 
                                                   this::showAlert);

        dashboardVC = new DashboardViewController(dashboardContent, authService, auctionService, 
                                                  () -> { showView(viewAuth); authVC.showSignInTab(); });

        createVC = new CreateAuctionViewController(createTitle, createDesc, createCategory, createCondition, 
                                                   createStartPrice, createDuration, createError, 
                                                   authService, auctionService, 
                                                   () -> showView(viewAuth), this::openAuctionDetail, homeVC::updateStats);

        homeVC.updateStats();
        homeVC.loadAuctionCards();
    }

    // ================================================================
    // NAVIGATION & EVENT DELEGATION
    // ================================================================

    private void showView(ScrollPane view) {
        for (ScrollPane v : new ScrollPane[]{viewHome, viewAuth, viewAuction, viewDashboard, viewCreate}) {
            if (v != null) { v.setVisible(v == view); v.setManaged(v == view); }
        }
    }

    @FXML private void onBrowse() { showView(viewHome); }
    @FXML private void onExploreAuctions() { showView(viewHome); homeVC.scrollToAuctions(); }
    @FXML private void onSearchChanged() { homeVC.onSearchChanged(); }
    @FXML private void onFilterCategory(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        homeVC.onFilterCategory(btn, btn.getText());
    }

    @FXML private void onSignIn() {
        if (authService.isLoggedIn()) { showView(viewDashboard); dashboardVC.renderDashboard(); }
        else { showView(viewAuth); authVC.showSignInTab(); }
    }

    @FXML private void onTabSignIn() { authVC.showSignInTab(); }
    @FXML private void onTabCreateAccount() { authVC.showCreateAccountTab(); }
    @FXML private void onLogin() { authVC.onLogin(); }
    @FXML private void onRegister() { authVC.onRegister(); }

    @FXML private void onStartSelling() {
        if (authService.isLoggedIn()) showView(viewCreate);
        else { showView(viewAuth); authVC.showSignInTab(); }
    }

    @FXML private void onCreateAuction() { createVC.onCreateAuction(); }
    @FXML private void onBackToAuctions() { 
        if (currentAuction != null) currentAuction.removeObserver(this);
        showView(viewHome); 
    }

    private void openAuctionDetail(String auctionId) {
        Auction auction = auctionService.getAuction(auctionId);
        if (auction == null) return;
        if (currentAuction != null) currentAuction.removeObserver(this);
        currentAuction = auction;
        currentAuction.addObserver(this);
        detailVC.setCurrentAuction(currentAuction);
        detailVC.renderAuctionDetail();
        showView(viewAuction);
    }

    private void onLoginSuccess(User user) {
        navUserLabel.setText(user.getFirstName());
        navUserLabel.setVisible(true); navUserLabel.setManaged(true);
        btnSignIn.setText("Dashboard");
        btnSignIn.setOnAction(e -> { showView(viewDashboard); dashboardVC.renderDashboard(); });
        showView(viewHome);
        homeVC.updateStats();
    }

    @Override
    public void onNewBid(Auction auction, BidTransaction bid) {
        Platform.runLater(() -> {
            if (currentAuction != null && currentAuction.getId().equals(auction.getId())) detailVC.renderAuctionDetail();
            homeVC.updateStats(); homeVC.loadAuctionCards();
        });
    }

    @Override
    public void onStateChange(Auction auction, AuctionState oldS, AuctionState newS) {
        Platform.runLater(() -> {
            if (currentAuction != null && currentAuction.getId().equals(auction.getId())) detailVC.renderAuctionDetail();
            homeVC.loadAuctionCards();
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
