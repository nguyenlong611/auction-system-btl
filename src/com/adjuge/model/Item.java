package com.adjuge.model;

public abstract class Item extends Entity {

    private String name;

    private String description;

    private final double startPrice;

    private final String imageUrl;

    private final String condition;

    private final Category category;

    public Item(String id, String name, String description, double startPrice,
                String imageUrl, String condition, Category category) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.category = category;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCondition() {
        return condition;
    }

    public Category getCategory(){ return category;}

    // ── Setters (only for fields that may change after creation) ─────────

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ── Abstract methods — POLYMORPHISM ──────────────────────────────────

    public abstract String getItemSpecifics();

    // ── Concrete overrides from Entity ───────────────────────────────────

    @Override
    public String printInfo() {
        return String.format("[%s] %s — %s | Condition: %s | Start: $%.2f\n  Details: %s",
                category, name, description, condition, startPrice, getItemSpecifics());
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
