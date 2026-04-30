package com.adjuge.model;

public class Admin extends User implements ManageUsersAble {

    public Admin(String id, String firstName, String lastName, String email, String password) {
        super(id, firstName, lastName, email, password);
    }

    // ── POLYMORPHISM overrides ───────────────────────────────────────────

    /** Returns "ADMIN" as this user's role. */
    @Override
    public String getRole() {
        return "ADMIN";
    }


    /** Admins CAN manage other users (ban, verify, etc.). */
    public boolean canManageUsers() {
        return true;
    }

    /**
     * Returns a summary string with the admin's name and email.
     */
    @Override
    public String printInfo() {
        return String.format("Admin: %s | Email: %s", getFullName(), getEmail());
    }
}
