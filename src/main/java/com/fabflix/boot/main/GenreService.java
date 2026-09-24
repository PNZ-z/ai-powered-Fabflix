package com.fabflix.boot.main;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class GenreService {
    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<Genre> listGenres() {
        return genreRepository.findAllSortedByName();
    }
}
