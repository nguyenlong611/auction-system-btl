package com.adjuge.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for time formatting and currency display.
 *
 * Provides helper methods used throughout the UI to show:
 *   - Human-readable dates ("Mar 28, 2026")
 *   - Relative timestamps ("5m ago", "2d ago")
 *   - Countdown timers ("3d 12h", "45m 30s")
 *   - Currency formatting ("$12,500")
 *
 * All methods are static — no need to create an instance.
 */
public class TimeUtil {

    // Date format used across the app (e.g., "Mar 28, 2026")
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    /**
     * Formats a LocalDateTime as a short date string.
     * Example: LocalDateTime of 2026-03-28 -> "Mar 28, 2026"
     */
    public static String formatDate(LocalDateTime dt) {
        return dt.format(DATE_FORMAT);
    }

    /**
     * Returns a human-readable "time ago" string.
     * Examples: "just now", "5m ago", "3h ago", "2d ago"
     *
     * @param ts the timestamp to compare against the current time
     */
    public static String timeAgo(LocalDateTime ts) {
        Duration d = Duration.between(ts, LocalDateTime.now());

        if (d.toMinutes() < 1) return "just now";
        if (d.toHours() < 1)   return d.toMinutes() + "m ago";
        if (d.toDays() < 1)    return d.toHours() + "h ago";
        return d.toDays() + "d ago";
    }

    /**
     * Returns a short countdown string for auction timers.
     * Shows the two most significant units:
     *   - "3d 12h" (days + hours)
     *   - "5h 30m" (hours + minutes)
     *   - "12m 45s" (minutes + seconds)
     *   - "Ended" if the time has passed
     *
     * @param endTime the auction end time
     */
    public static String shortTimer(LocalDateTime endTime) {
        Duration d = Duration.between(LocalDateTime.now(), endTime);

        // Auction has already ended
        if (d.isNegative()) return "Ended";

        long days  = d.toDays();
        long hours = d.toHours() % 24;
        long mins  = d.toMinutes() % 60;
        long secs  = d.getSeconds() % 60;

        if (days > 0)  return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m " + secs + "s";
    }

    /**
     * Checks if an auction is ending soon (within 3 hours).
     * Used to show urgency indicators in the UI.
     *
     * @param endTime the auction end time
     * @return true if less than 3 hours remain
     */
    public static boolean isEndingSoon(LocalDateTime endTime) {
        return Duration.between(LocalDateTime.now(), endTime).toHours() < 3;
    }

    /**
     * Formats a dollar amount with commas and no decimals.
     * Examples: 12500.0 -> "$12,500", 1000000.0 -> "$1,000,000"
     *
     * @param amount the dollar amount
     */
    public static String formatCurrency(double amount) {
        return "$" + String.format("%,.0f", amount);
    }

    /**
     * Breaks down the remaining time into days, hours, minutes, seconds.
     * Returns a 4-element array: [days, hours, minutes, seconds].
     * Returns all zeros if the end time has passed.
     *
     * Useful for building countdown displays in the UI.
     *
     * @param endTime the auction end time
     */
    public static long[] getCountdownParts(LocalDateTime endTime) {
        Duration d = Duration.between(LocalDateTime.now(), endTime);

        // If time has passed, return all zeros
        if (d.isNegative()) {
            return new long[]{0, 0, 0, 0};
        }

        return new long[]{
                d.toDays(),            // days remaining
                d.toHours() % 24,     // hours (0-23)
                d.toMinutes() % 60,   // minutes (0-59)
                d.getSeconds() % 60   // seconds (0-59)
        };
    }
}
