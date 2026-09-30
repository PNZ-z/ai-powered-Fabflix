package com.fabflix.boot.movie;

import org.springframework.stereotype.Service;

@Service
public class MovieDetailService {
    private final MovieDetailRepository movieDetailRepository;

    public MovieDetailService(MovieDetailRepository movieDetailRepository) {
        this.movieDetailRepository = movieDetailRepository;
    }

    public MovieDetail getMovie(String movieId) {
        String normalizedMovieId = movieId == null ? null : movieId.trim();
        if (normalizedMovieId == null || normalizedMovieId.isEmpty()) {
            throw new IllegalArgumentException("Movie id is required.");
        }

        return movieDetailRepository.findById(normalizedMovieId)
                .orElseThrow(() -> new MovieNotFoundException(normalizedMovieId));
    }
}
