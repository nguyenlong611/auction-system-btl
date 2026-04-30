package com.adjuge.model;

/**
 * Represents an art or collectible item (paintings, sculptures, rare prints, etc.).
 * Extends Item and automatically sets the category to ART_COLLECTIBLES.
 *
 * POLYMORPHISM: getCategoryLabel() returns "ART & COLLECTIBLES" and
 * getItemSpecifics() returns the artist, year, and medium.
 */
public class Art extends Item {

    /** The name of the artist who created the piece. */
    private final String artist;

    /** The year the artwork was created. */
    private final int year;

    /** The artistic medium (e.g. "Oil on Canvas", "Bronze", "Watercolor"). */
    private final String medium;

    /**
     * Creates a new Art item.
     *
     * @param id          unique identifier
     * @param name        item title
     * @param description detailed description
     * @param startPrice  minimum starting price
     * @param imageUrl    URL to an image
     * @param condition   item condition
     * @param artist      name of the artist
     * @param year        year of creation
     * @param medium      artistic medium
     */
    public Art(String id, String name, String description, double startPrice,
               String imageUrl, String condition,
               String artist, int year, String medium) {
        super(id, name, description, startPrice, imageUrl, condition, Category.ART_COLLECTIBLES);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    public String getMedium() {
        return medium;
    }

    /**
     * POLYMORPHISM override — returns art-specific details.
     */
    @Override
    public String getItemSpecifics() {
        return "Họa sĩ: " + artist + " | Year: " + year + " | Medium: " + medium;
    }

    public static class ArtBuilder {
        // Các thuộc tính chung của Item
        private String id;
        private String name;
        private String description;
        private double startPrice;
        private String imageUrl;
        private String condition;

        // Các thuộc tính riêng của Art
        private String artist;
        private int year;
        private String medium;

        public ArtBuilder setId(String id) { this.id = id; return this; }
        public ArtBuilder setName(String name) { this.name = name; return this; }
        public ArtBuilder setDescription(String description) { this.description = description; return this; }
        public ArtBuilder setStartPrice(double startPrice) { this.startPrice = startPrice; return this; }
        public ArtBuilder setImageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public ArtBuilder setCondition(String condition) { this.condition = condition; return this; }

        public ArtBuilder setArtist(String artist) { this.artist = artist; return this; }
        public ArtBuilder setYear(int year) { this.year = year; return this; }
        public ArtBuilder setMedium(String medium) { this.medium = medium; return this; }

        // Hàm chốt hạ: Đúc ra đối tượng Art bằng Constructor đã có
        public Art build() {
            return new Art(id, name, description, startPrice, imageUrl, condition, artist, year, medium);
        }
    }
}
