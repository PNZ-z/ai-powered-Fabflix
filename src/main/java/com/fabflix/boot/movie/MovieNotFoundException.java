package com.fabflix.boot.movie;

public class MovieNotFoundException extends RuntimeException {
    public MovieNotFoundException(String movieId) {
        super("Movie not found: " + movieId);
    }
}
