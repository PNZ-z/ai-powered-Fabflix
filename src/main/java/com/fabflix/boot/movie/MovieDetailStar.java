package com.fabflix.boot.movie;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieDetailStar {
    private final String starId;
    private final String starName;

    public MovieDetailStar(String starId, String starName) {
        this.starId = starId;
        this.starName = starName;
    }

    @JsonProperty("star_id")
    public String getStarId() {
        return starId;
    }

    @JsonProperty("star_name")
    public String getStarName() {
        return starName;
    }
}
