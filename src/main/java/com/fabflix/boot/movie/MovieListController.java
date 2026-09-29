package com.fabflix.boot.movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieListController {
    private static final Logger logger = LoggerFactory.getLogger(MovieListController.class);

    private final MovieListService movieListService;

    public MovieListController(MovieListService movieListService) {
        this.movieListService = movieListService;
    }

    @GetMapping("/api/boot/movie-list")
    public ResponseEntity<?> listMovies(@ModelAttribute MovieListRequest request) {
        try {
            List<MovieSummary> movies = movieListService.listMovies(request);
            logger.info("Movie list request completed: resultCount={}", movies.size());
            return ResponseEntity.ok(movies);
        } catch (RuntimeException exception) {
            logger.error("Movie list request failed", exception);
            Map<String, String> error = new HashMap<>();
            error.put("errorMessage", exception.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
        }
    }
}
