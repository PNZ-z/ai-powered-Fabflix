package com.fabflix.boot.movie;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieSummaryStar {
    private String starId;
    private String starName;

    public MovieSummaryStar() {
        // Keep an empty constructor so frameworks and tests can create the object first,
        // then fill starId and starName through setters.
    }

    public MovieSummaryStar(String starId, String starName) {
        // Store the two values that make up one item inside the legacy movie_stars array.
        this.starId = starId;
        this.starName = starName;
    }

    @JsonProperty("star_id")
    public String getStarId() {
        // Return the star_id value expected by the legacy frontend contract.
        return starId;
    }

    public void setStarId(String starId) {
        // Store the star id from the database row.
        this.starId = starId;
    }

    @JsonProperty("star_name")
    public String getStarName() {
        // Return the star_name value expected by the legacy frontend contract.
        return starName;
    }

    public void setStarName(String starName) {
        // Store the star name from the database row.
        this.starName = starName;
    }
}
