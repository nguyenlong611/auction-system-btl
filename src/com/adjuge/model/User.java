package com.adjuge.model;

import java.time.LocalDateTime;

public abstract class User extends Entity {

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private final LocalDateTime memberSince;

    public User(String id, String firstName, String lastName, String email, String password) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.memberSince = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getMemberSince() {
        return memberSince;
    }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ── Convenience methods ──────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        return ("" + firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
    }

    // ── Abstract methods — POLYMORPHISM ──────────────────────────────────

    public abstract String getRole();
    // ── Concrete override from Entity ────────────────────────────────────

    @Override
    public String getDisplayName() {
        return getFullName();
    }
}
