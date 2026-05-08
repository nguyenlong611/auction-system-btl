package com.adjuge.controller;

import com.adjuge.model.*;
import com.adjuge.pattern.ItemFactory;
import com.adjuge.service.AuctionService;
import com.adjuge.service.AuthService;

import javafx.scene.control.*;

import java.util.function.Consumer;

/**
 * Sub-controller responsible for the Create Auction view (SRP).
 *
 * Demonstrates:
 *  - POLYMORPHISM: user.canSell() overridden per User subtype
 *  - Factory Method Pattern: ItemFactory.createItem() selects Item subclass
 *  - Exception Handling: NumberFormatException on price field
 *  - Generics: ComboBox<String> fields
 */
public class CreateAuctionViewController {

    // Generics: ComboBox<String>
    private final TextField        createTitle;
    private final TextArea         createDesc;
    private final ComboBox<String> createCategory;
    private final ComboBox<String> createCondition;
    private final TextField        createStartPrice;
    private final ComboBox<String> createDuration;
    private final Label            createError;

    private final AuthService      authService;
    private final AuctionService   auctionService;
    private final Runnable         onRedirectToAuth;
    private final Consumer<String> onAuctionCreated;
    private final Runnable         onRefreshStats;

    public CreateAuctionViewController(
            TextField createTitle, TextArea createDesc,
            ComboBox<String> createCategory, ComboBox<String> createCondition,
            TextField createStartPrice, ComboBox<String> createDuration, Label createError,
            AuthService authService, AuctionService auctionService,
            Runnable onRedirectToAuth, Consumer<String> onAuctionCreated, Runnable onRefreshStats) {
        this.createTitle      = createTitle;
        this.createDesc       = createDesc;
        this.createCategory   = createCategory;
        this.createCondition  = createCondition;
        this.createStartPrice = createStartPrice;
        this.createDuration   = createDuration;
        this.createError      = createError;
        this.authService      = authService;
        this.auctionService   = auctionService;
        this.onRedirectToAuth = onRedirectToAuth;
        this.onAuctionCreated = onAuctionCreated;
        this.onRefreshStats   = onRefreshStats;
    }

    /**
     * Validate form and create a new auction listing.
     *
     * POLYMORPHISM: checks if user implements Sellable interface (Seller/Admin do, Bidder does not).
     * Factory Method: ItemFactory.createItem() picks the right Item subclass.
     * Exception Handling: NumberFormatException caught for price field.
     */
    public void onCreateAuction() {
        User user = authService.getCurrentUser();
        if (user == null) { onRedirectToAuth.run(); return; }

        // POLYMORPHISM via Interface: only users implementing Sellable (e.g. Seller) can create listings
        if (!(user instanceof Sellable) || !((Sellable) user).canSell()) {
            createError.setText("Your account type (" + user.getRole() + ") cannot create listings.");
            return;
        }

        String title     = createTitle.getText().trim();
        String desc      = createDesc.getText().trim();
        String catStr    = createCategory.getValue();
        String condition = createCondition.getValue();
        String priceStr  = createStartPrice.getText().trim();
        String durStr    = createDuration.getValue();

        if (title.isEmpty() || desc.isEmpty() || catStr == null || priceStr.isEmpty()) {
            createError.setText("Please fill in all required fields."); return;
        }

        double price;
        try {
            // Exception Handling: non-numeric input
            price = Double.parseDouble(priceStr);
            if (price < 1) { createError.setText("Starting bid must be at least $1."); return; }
        } catch (NumberFormatException e) {
            createError.setText("Invalid price format."); return;
        }

        int days = Integer.parseInt(durStr.replaceAll("[^0-9]", ""));

        // Factory Method Pattern: correct Item subclass selected by category
        Category category = Category.fromDisplayName(catStr);
        Item item = ItemFactory.createItem(category, title, desc, price,
                "https://picsum.photos/seed/" + System.currentTimeMillis() + "/800/600",
                condition != null ? condition : "Good");

        // Strategy Pattern: ValidatorType selects the bid validation algorithm
        Auction auction = auctionService.createAuction(item, user.getId(),
                user.getFirstName() + " " + user.getLastName().charAt(0) + ".",
                days, ValidatorType.STANDARD);

        createError.setText("");
        createTitle.clear(); createDesc.clear(); createStartPrice.clear();
        onRefreshStats.run();
        onAuctionCreated.accept(auction.getId());
    }
}
