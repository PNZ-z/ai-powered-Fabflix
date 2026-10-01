package com.fabflix.boot.star;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StarDetailMovie {
    private final String movieId;
    private final String movieTitle;

    public StarDetailMovie(String movieId, String movieTitle) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
    }

    @JsonProperty("movie_id")
    public String getMovieId() {
        return movieId;
    }

    @JsonProperty("movie_title")
    public String getMovieTitle() {
        return movieTitle;
    }
}
