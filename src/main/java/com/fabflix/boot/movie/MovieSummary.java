package com.fabflix.boot.movie;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MovieSummary {
    private String movieId;
    private String movieTitle;
    private int movieYear;
    private String movieDirector;
    private String movieGenres;
    private Float movieRating;
    private List<MovieSummaryStar> movieStars;

    public MovieSummary() {
    }

    public MovieSummary(
            String movieId,
            String movieTitle,
            int movieYear,
            String movieDirector,
            String movieGenres,
            Float movieRating,
            List<MovieSummaryStar> movieStars) {
        setMovieId(movieId);
        setMovieTitle(movieTitle);
        setMovieYear(movieYear);
        setMovieDirector(movieDirector);
        setMovieGenres(movieGenres);
        setMovieRating(movieRating);
        setMovieStars(movieStars);
    }

    @JsonProperty("movie_id")
    public String getMovieId() {
        // Return the movie_id value expected by the legacy frontend contract.
        return movieId;
    }

    public void setMovieId(String movieId) {
        // Store the movie_id value from the database row.
        this.movieId = movieId;
    }

    @JsonProperty("movie_title")
    public String getMovieTitle() {
        // Return the movie_title value expected by the legacy frontend contract.
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        // Store the movie title from the database row.
        this.movieTitle = movieTitle;
    }

    @JsonProperty("movie_year")
    public int getMovieYear() {
        // Return the movie_year value expected by the legacy frontend contract.
        return movieYear;
    }

    public void setMovieYear(int movieYear) {
        // Store the movie year from the database row.
        this.movieYear = movieYear;
    }

    @JsonProperty("movie_director")
    public String getMovieDirector() {
        // Return the movie_director value expected by the legacy frontend contract.
        return movieDirector;
    }

    public void setMovieDirector(String movieDirector) {
        // Store the movie director from the database row.
        this.movieDirector = movieDirector;
    }

    @JsonProperty("movie_genres")
    public String getMovieGenres() {
        // Return the comma-separated top genres expected by the legacy frontend contract.
        return movieGenres;
    }

    public void setMovieGenres(String movieGenres) {
        // Store the comma-separated top genres from the database row.
        this.movieGenres = movieGenres;
    }

    @JsonProperty("movie_rating")
    public Float getMovieRating() {
        // Return null when the database rating is null, otherwise return the rating value.
        return movieRating;
    }

    public void setMovieRating(Float movieRating) {
        // Store the nullable movie rating from the database row.
        this.movieRating = movieRating;
    }

    @JsonProperty("movie_stars")
    public List<MovieSummaryStar> getMovieStars() {
        // Return the top stars expected by the legacy frontend contract.
        return movieStars;
    }

    public void setMovieStars(List<MovieSummaryStar> movieStars) {
        // Store the top stars mapped from the database row.
        this.movieStars = movieStars;
    }
}
