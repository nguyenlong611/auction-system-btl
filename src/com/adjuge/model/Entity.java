package com.adjuge.model;

import java.time.LocalDateTime;

public abstract class Entity {

    private final String id;

    private final LocalDateTime createdAt;

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public abstract String printInfo();

    public abstract String getDisplayName();

    @Override
    public String toString() {
        return printInfo();
    }
}
