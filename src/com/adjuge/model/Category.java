package com.adjuge.model;

/**
 * Enum representing the categories an auction item can belong to.
 *
 * Each constant stores a human-readable display name (e.g. "Jewelry & Watches").
 * Use getDisplayName() to show the friendly label in the UI, and
 * fromDisplayName(String) to convert a label back into the enum constant.
 */
public enum Category {

    ELECTRONICS("Electronics"),
    ART_COLLECTIBLES("Art & Collectibles"),
    VEHICLES("Vehicles"),;

    /** The user-friendly name shown in the interface. */
    private final String displayName;

    /**
     * Private constructor — called once per constant above.
     *
     * @param displayName the friendly label for this category
     */
    Category(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the human-readable display name for this category. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Looks up a Category by its display name (case-insensitive).
     * For example, fromDisplayName("Electronics") returns Category.ELECTRONICS.
     *
     * @param displayName the friendly label to search for
     * @return the matching Category constant
     * @throws IllegalArgumentException if no category matches
     */
    public static Category fromDisplayName(String displayName) {
        for (Category category : values()) {
            if (category.displayName.equalsIgnoreCase(displayName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + displayName);
    }
}
