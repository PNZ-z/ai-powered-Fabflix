package com.fabflix.boot.search;

import java.util.Map;

public class AutocompleteSuggestion {
    private final String value;
    private final Map<String, String> data;

    public AutocompleteSuggestion(String movieId, String title) {
        this.value = title;
        this.data = Map.of("movie_id", movieId);
    }

    public String getValue() { return value; }
    public Map<String, String> getData() { return data; }
}
