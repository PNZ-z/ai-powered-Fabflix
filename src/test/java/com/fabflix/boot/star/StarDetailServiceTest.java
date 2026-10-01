package com.fabflix.boot.star;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarDetailServiceTest {
    @Test
    void trimsStarIdBeforeCallingRepository() {
        StarDetailRepository repository = mock(StarDetailRepository.class);
        StarDetail star = star("nm0000244");
        when(repository.findById("nm0000244")).thenReturn(Optional.of(star));

        StarDetail result = new StarDetailService(repository).getStar("  nm0000244  ");

        assertSame(star, result);
        verify(repository).findById("nm0000244");
    }

    @Test
    void rejectsMissingStarId() {
        StarDetailService service = new StarDetailService(mock(StarDetailRepository.class));

        IllegalArgumentException nullFailure = assertThrows(
                IllegalArgumentException.class,
                () -> service.getStar(null)
        );
        assertEquals("Star id is required.", nullFailure.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.getStar("   "));
    }

    @Test
    void throwsNotFoundWhenRepositoryReturnsEmpty() {
        StarDetailRepository repository = mock(StarDetailRepository.class);
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        StarNotFoundException failure = assertThrows(
                StarNotFoundException.class,
                () -> new StarDetailService(repository).getStar("missing-id")
        );

        assertEquals("Star not found: missing-id", failure.getMessage());
    }

    private StarDetail star(String id) {
        return new StarDetail(id, "Sigourney Weaver", 1949,
                List.of(new StarDetailMovie("tt0078748", "Alien")));
    }
}
