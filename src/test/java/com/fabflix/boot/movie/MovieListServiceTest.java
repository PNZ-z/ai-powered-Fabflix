package com.fabflix.boot.movie;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MovieListServiceTest {
    @Test
    void normalizesRequestBeforeCallingRepository() {
        MovieListQueryFactory queryFactory = mock(MovieListQueryFactory.class);
        MovieListRepository repository = mock(MovieListRepository.class);

        MovieListRequest request = mock(MovieListRequest.class);
        MovieListQuery query = mock(MovieListQuery.class);
        List<MovieSummary> expectedMovies = List.of(new MovieSummary());

        when(queryFactory.fromRequest(request)).thenReturn(query);
        when(repository.findMovies(query)).thenReturn(expectedMovies);

        MovieListService service = new MovieListService(queryFactory, repository);

        List<MovieSummary> actualMovies = service.listMovies(request);

        assertSame(actualMovies, expectedMovies);

        verify(queryFactory).fromRequest(request);
        verify(repository).findMovies(query);



    }
}
