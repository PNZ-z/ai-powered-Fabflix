package com.fabflix.boot.main;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenreServiceTest {
    @Test
    void returnsGenresReportedByRepository() {
        GenreRepository genreRepository = mock(GenreRepository.class);
        List<Genre> genres = List.of(new Genre("Action"), new Genre("Comedy"));
        when(genreRepository.findAllSortedByName()).thenReturn(genres);

        List<Genre> result = new GenreService(genreRepository).listGenres();

        assertSame(genres, result);
        verify(genreRepository).findAllSortedByName();
    }
}
