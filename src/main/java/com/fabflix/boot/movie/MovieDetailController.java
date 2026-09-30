package com.fabflix.boot.movie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieDetailController {
    private static final Logger logger = LoggerFactory.getLogger(MovieDetailController.class);

    private final MovieDetailService movieDetailService;

    public MovieDetailController(MovieDetailService movieDetailService) {
        this.movieDetailService = movieDetailService;
    }

    @GetMapping("/api/boot/single-movie")
    public ResponseEntity<MovieDetail> getMovie(
            @RequestParam(value = "id", required = false) String movieId) {
        MovieDetail movie = movieDetailService.getMovie(movieId);
        logger.info("Movie detail request completed: movieId={}", movie.getMovieId());
        return ResponseEntity.ok(movie);
    }
}
