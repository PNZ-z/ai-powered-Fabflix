package com.fabflix.boot.main;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    private final GenreService genreService;

    public MainController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping("/api/boot/main")
    public ResponseEntity<?> listGenres() {
        try {
            List<Genre> genres = genreService.listGenres();
            return ResponseEntity.ok(genres);
        } catch (RuntimeException exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errorMessage", exception.getMessage()));
        }
    }
}
