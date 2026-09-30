package com.fabflix.boot.movie;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovieDetailServiceTest {
    @Test
    void trimsMovieIdBeforeCallingRepository() {
        MovieDetailRepository repository = mock(MovieDetailRepository.class);
        MovieDetail movie = movie("tt0078748");
        when(repository.findById("tt0078748")).thenReturn(Optional.of(movie));

        MovieDetail result = new MovieDetailService(repository).getMovie("  tt0078748  ");

        assertSame(movie, result);
        verify(repository).findById("tt0078748");
    }

    @Test
    void rejectsMissingMovieId() {
        MovieDetailService service = new MovieDetailService(mock(MovieDetailRepository.class));

        IllegalArgumentException nullFailure = assertThrows(
                IllegalArgumentException.class,
                () -> service.getMovie(null)
        );
        assertEquals("Movie id is required.", nullFailure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.getMovie("   "));
    }

    @Test
    void throwsNotFoundWhenRepositoryReturnsEmpty() {
        MovieDetailRepository repository = mock(MovieDetailRepository.class);
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        MovieNotFoundException failure = assertThrows(
                MovieNotFoundException.class,
                () -> new MovieDetailService(repository).getMovie("missing-id")
        );

        assertEquals("Movie not found: missing-id", failure.getMessage());
    }

    private MovieDetail movie(String id) {
        return new MovieDetail(id, "Alien", 1979, "Ridley Scott", "Horror", "8.5",
                List.of(new MovieDetailStar("nm0000244", "Sigourney Weaver")));
    }
}
