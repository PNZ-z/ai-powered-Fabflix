package com.fabflix.boot.star;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StarDetail {
    private final String starId;
    private final String starName;
    private final Object starBirthYear;
    private final List<StarDetailMovie> starMovies;

    public StarDetail(
            String starId,
            String starName,
            Object starBirthYear,
            List<StarDetailMovie> starMovies) {
        this.starId = starId;
        this.starName = starName;
        this.starBirthYear = starBirthYear;
        this.starMovies = starMovies;
    }

    @JsonProperty("star_id")
    public String getStarId() {
        return starId;
    }

    @JsonProperty("star_name")
    public String getStarName() {
        return starName;
    }

    @JsonProperty("star_birth_year")
    public Object getStarBirthYear() {
        return starBirthYear;
    }

    @JsonProperty("star_movies")
    public List<StarDetailMovie> getStarMovies() {
        return starMovies;
    }
}
