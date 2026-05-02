package com.adjuge.service;

import java.util.UUID;

import com.adjuge.exception.AuthenticationException;
import com.adjuge.model.Seller;
import com.adjuge.model.User;
import com.adjuge.pattern.DataStore;

/**
 * Handles user authentication: login, registration, and session management.
 *
 * This service keeps track of who is currently logged in by storing a reference
 * to the active User object. When no one is logged in, currentUser is null.
 */
public class AuthService {
    // The currently logged-in user (null means no one is logged in)
    private User currentUser;

    /**
     * Attempts to log a user in with their email and password.
     *
     * How it works:
     * 1. Look up the user by email in the DataStore
     * 2. If not found, or if the password doesn't match, throw an exception
     * 3. Otherwise, store the user as the current session user and return them
     *
     * @param email    the user's email address
     * @param password the user's plain-text password
     * @return the authenticated User object
     * @throws AuthenticationException if the email is not found or the password is wrong
     */
    public User login(String email, String password) throws AuthenticationException {
        // Look up the user by email in our data store
        User user = DataStore.getInstance().getUserByEmail(email);

        // If user doesn't exist OR password doesn't match, reject the login
        if (user == null || !user.getPassword().equals(password)) {
            throw new AuthenticationException(
                "Invalid email or password. Try demo@bidvault.com / password123"
            );
        }

        // Success — save the user as the current session user
        currentUser = user;
        return user;
    }
    public User register(String firstName, String lastName, String email,
                         String password, String confirmPassword) throws AuthenticationException {

        // --- Validation checks ---

        // Check that both password fields match
        if (!password.equals(confirmPassword)) {
            throw new AuthenticationException("Passwords do not match.");
        }

        // Enforce minimum password length for basic security
        if (password.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters.");
        }

        // Make sure the email isn't already taken by another account
        if (DataStore.getInstance().getUserByEmail(email) != null) {
            throw new AuthenticationException("An account with this email already exists.");
        }

        // --- Create the new user ---

        // Generate a unique ID like "u_3fa85f64"
        String id = "u_" + UUID.randomUUID().toString().substring(0, 8);

        // Create a Seller account (sellers can also bid on auctions)
        // Parameters: id, firstName, lastName, email, password, verified, totalListings
        Seller user = new Seller(id, firstName, lastName, email.toLowerCase(), password);

        // Save the new user to the data store
        DataStore.getInstance().addUser(user);

        // Automatically log the new user in after registration
        currentUser = user;
        return user;
    }

    /**
     * Logs the current user out by clearing the session.
     */
    public void logout() {
        currentUser = null;
    }

    /**
     * Returns the currently logged-in user, or null if no one is logged in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Quick check to see if someone is logged in.
     *
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
