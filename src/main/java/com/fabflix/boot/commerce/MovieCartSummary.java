package com.fabflix.boot.commerce;

public class MovieCartSummary {
    private final String id;
    private final String title;

    public MovieCartSummary(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
