package com.adjuge.model;

/**
 * Represents a vehicle item (cars, motorcycles, boats, etc.).
 * Extends Item and automatically sets the category to VEHICLES.
 *
 * POLYMORPHISM: getCategoryLabel() returns "VEHICLES" and getItemSpecifics()
 * returns the year, make, model, and mileage.
 */
public class Vehicle extends Item {

    /** The year the vehicle was manufactured. */
    private final int yearMade;

    /** The manufacturer (e.g. "Toyota", "Ford"). */
    private final String make;

    /** The specific vehicle model (e.g. "Camry", "Mustang"). */
    private final String vehicleModel;

    /** The odometer reading in miles. */
    private final int mileage;

    /**
     * Creates a new Vehicle item.
     *
     * @param id           unique identifier
     * @param name         item title
     * @param description  detailed description
     * @param startPrice   minimum starting price
     * @param imageUrl     URL to an image
     * @param condition    item condition
     * @param yearMade     manufacturing year
     * @param make         manufacturer name
     * @param vehicleModel model name
     * @param mileage      odometer reading in miles
     */
    public Vehicle(String id, String name, String description, double startPrice,
                   String imageUrl, String condition,
                   int yearMade, String make, String vehicleModel, int mileage) {
        super(id, name, description, startPrice, imageUrl, condition, Category.VEHICLES);
        this.yearMade = yearMade;
        this.make = make;
        this.vehicleModel = vehicleModel;
        this.mileage = mileage;
    }

    public int getYearMade() {
        return yearMade;
    }

    public String getMake() {
        return make;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public int getMileage() {
        return mileage;
    }

    /**
     * POLYMORPHISM override — returns vehicle-specific details.
     * Format: "YYYY Make Model | X mi"
     */
    @Override
    public String getItemSpecifics() {
        return yearMade + " " + make + " " + vehicleModel + " | " + mileage + " mi";
    }

    public static class VehicleBuilder {
        // Các thuộc tính chung của Item
        private String id;
        private String name;
        private String description;
        private double startPrice;
        private String imageUrl;
        private String condition;

        // Các thuộc tính riêng của Vehicle
        private int yearMade;
        private String make;
        private String vehicleModel;
        private int mileage;

        public VehicleBuilder setId(String id) { this.id = id; return this; }
        public VehicleBuilder setName(String name) { this.name = name; return this; }
        public VehicleBuilder setDescription(String description) { this.description = description; return this; }
        public VehicleBuilder setStartPrice(double startPrice) { this.startPrice = startPrice; return this; }
        public VehicleBuilder setImageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public VehicleBuilder setCondition(String condition) { this.condition = condition; return this; }

        public VehicleBuilder setYearMade(int yearMade) { this.yearMade = yearMade; return this; }
        public VehicleBuilder setMake(String make) { this.make = make; return this; }
        public VehicleBuilder setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; return this; }
        public VehicleBuilder setMileage(int mileage) { this.mileage = mileage; return this; }

        // Hàm chốt hạ: Đúc ra đối tượng Vehicle bằng Constructor đã có
        public Vehicle build() {
            return new Vehicle(id, name, description, startPrice, imageUrl, condition, yearMade, make, vehicleModel, mileage);
        }
    }
}

