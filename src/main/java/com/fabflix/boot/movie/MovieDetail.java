package com.fabflix.boot.movie;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieDetail {
    private final String movieId;
    private final String movieTitle;
    private final int movieYear;
    private final String movieDirector;
    private final String movieGenres;
    private final String movieRating;
    private final List<MovieDetailStar> movieStars;

    public MovieDetail(
            String movieId,
            String movieTitle,
            int movieYear,
            String movieDirector,
            String movieGenres,
            String movieRating,
            List<MovieDetailStar> movieStars) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.movieYear = movieYear;
        this.movieDirector = movieDirector;
        this.movieGenres = movieGenres;
        this.movieRating = movieRating;
        this.movieStars = movieStars;
    }

    @JsonProperty("movie_id")
    public String getMovieId() {
        return movieId;
    }

    @JsonProperty("movie_title")
    public String getMovieTitle() {
        return movieTitle;
    }

    @JsonProperty("movie_year")
    public int getMovieYear() {
        return movieYear;
    }

    @JsonProperty("movie_director")
    public String getMovieDirector() {
        return movieDirector;
    }

    @JsonProperty("movie_genres")
    public String getMovieGenres() {
        return movieGenres;
    }

    @JsonProperty("movie_rating")
    public String getMovieRating() {
        return movieRating;
    }

    @JsonProperty("movie_stars")
    public List<MovieDetailStar> getMovieStars() {
        return movieStars;
    }
}
