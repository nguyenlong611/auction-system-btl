package com.adjuge.model;

public class Electronics extends Item {

    /** The manufacturer brand (e.g. "Sony", "Apple"). */
    private final String brand;

    /** The product model name or number (e.g. "iPhone 15 Pro"). */
    private final String model;

    /** Warranty duration in months (0 means no warranty). */
    private final int warrantyMonths;

    public Electronics(String id, String name, String description, double startPrice,
                       String imageUrl, String condition,
                       String brand, String model, int warrantyMonths) {
        // Pass Category.ELECTRONICS to the Item constructor
        super(id, name, description, startPrice, imageUrl, condition, Category.ELECTRONICS);
        this.brand = brand;
        this.model = model;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    /**
     * POLYMORPHISM override — returns electronics-specific details.
     */
    @Override
    public String getItemSpecifics() {
        return "Brand: " + brand + " | Model: " + model + " | Warranty: " + warrantyMonths + "mo";
    }
    public static class ElectronicsBuilder {
        // Các thuộc tính chung của Item
        private String id;
        private String name;
        private String description;
        private double startPrice;
        private String imageUrl;
        private String condition;

        // Các thuộc tính riêng của Electronics
        private String brand;
        private String model;
        private int warrantyMonths;

        public ElectronicsBuilder setId(String id) { this.id = id; return this; }
        public ElectronicsBuilder setName(String name) { this.name = name; return this; }
        public ElectronicsBuilder setDescription(String description) { this.description = description; return this; }
        public ElectronicsBuilder setStartPrice(double startPrice) { this.startPrice = startPrice; return this; }
        public ElectronicsBuilder setImageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ElectronicsBuilder setCondition(String condition) { this.condition = condition; return this; }

        public ElectronicsBuilder setBrand(String brand) { this.brand = brand; return this; }
        public ElectronicsBuilder setModel(String model) { this.model = model; return this; }
        public ElectronicsBuilder setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; return this; }

        // Hàm chốt hạ: Đúc ra đối tượng Electronics bằng Constructor đã có
        public Electronics build() {
            return new Electronics(id, name, description, startPrice, imageUrl, condition, brand, model, warrantyMonths);
        }
    }
}
