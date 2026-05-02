package com.adjuge.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.adjuge.model.Auction;
import com.adjuge.model.AuctionState;
import com.adjuge.pattern.DataStore;

/**
 * Runs a background thread that automatically closes expired auctions.
 *
 * Every second, this service checks all auctions in the system. If an auction
 * is in the RUNNING state but its end time has passed (isExpired() returns true),
 * it transitions the auction to the FINISHED state.
 *
 * WHY A BACKGROUND THREAD?
 * Auctions have a fixed end time. Without this service, an auction would only
 * be marked as finished when someone tries to interact with it (e.g., place a bid).
 * The timer ensures auctions close on time even if no one is actively viewing them,
 * and the UI updates automatically through the Observer pattern.
 *
 * WHY DAEMON THREAD?
 * A daemon thread is automatically stopped when the application exits. Without this,
 * the background thread would keep the JVM alive even after the user closes the window.
 */
public class AuctionTimerService {

    /**
     * A scheduled executor that runs our check task on a fixed interval.
     * We use a single thread since the check is lightweight and fast.
     * The thread is marked as a daemon so it won't prevent the app from exiting.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AuctionTimer");
        t.setDaemon(true);  // Daemon thread — exits automatically when the app closes
        return t;
    });

    /**
     * Starts the background timer that checks for expired auctions every second.
     *
     * The task runs with a fixed rate of 1 second:
     * - Initial delay: 0 seconds (starts immediately)
     * - Period: 1 second (runs every second after that)
     *
     * Each run iterates over all auctions and transitions any expired RUNNING
     * auctions to the FINISHED state. The setState() call on the auction will
     * notify any registered observers (e.g., the UI) so they can update.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Loop through every auction in the system
                for (Auction auction : DataStore.getInstance().getAllAuctions()) {

                    // Only check auctions that are currently RUNNING
                    if (auction.getState() == AuctionState.RUNNING && auction.isExpired()) {

                        // Transition the auction to FINISHED
                        // setState() notifies observers, so the UI updates automatically
                        auction.finish();

                        // Log to console for debugging purposes
                        System.out.println("[AuctionTimer] Auction closed: "
                            + auction.getItem().getName());
                    }
                }
            } catch (Exception e) {
                // Catch all exceptions so the scheduler doesn't stop on errors.
                // If we let an exception propagate, the scheduled task would be
                // cancelled and no more auctions would be auto-closed.
                System.err.println("[AuctionTimer] Error: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the background timer gracefully.
     * Call this when the application is shutting down to clean up resources.
     */
    public void stop() {
        scheduler.shutdown();
    }
}
