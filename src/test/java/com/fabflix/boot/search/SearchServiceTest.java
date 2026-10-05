package com.fabflix.boot.search;

import java.util.List;

import com.fabflix.boot.movie.MovieListQuery;
import com.fabflix.boot.movie.MovieListRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    @Test void skipsAutocompleteDatabaseForShortAndInvalidInput() {
        AutocompleteRepository autocomplete = mock(AutocompleteRepository.class);
        SearchService service = service(autocomplete, mock(MovieListRepository.class));
        assertTrue(service.autocomplete("ab").isEmpty());
        assertTrue(service.autocomplete("!!!").isEmpty());
        verify(autocomplete, never()).findSuggestions(org.mockito.ArgumentMatchers.any());
    }

    @Test void buildsCompatibleAutocompleteQuery() {
        AutocompleteRepository autocomplete = mock(AutocompleteRepository.class);
        when(autocomplete.findSuggestions("+Alien* +movie*")).thenReturn(List.of());
        service(autocomplete, mock(MovieListRepository.class)).autocomplete("Alien movie");
        verify(autocomplete).findSuggestions("+Alien* +movie*");
    }

    @Test void normalizesPagingAndSortingForFullTextSearch() {
        MovieListRepository repository = mock(MovieListRepository.class);
        SearchRequest request = new SearchRequest();
        request.setQuery("Alien!"); request.setPageSize("20"); request.setPageNumber("2");
        request.setSort1("rating"); request.setDir1("DESC");
        request.setSort2("rating"); request.setDir2("bad");
        service(mock(AutocompleteRepository.class), repository).search(request);
        ArgumentCaptor<MovieListQuery> captor = ArgumentCaptor.forClass(MovieListQuery.class);
        verify(repository).findMovies(captor.capture());
        MovieListQuery query = captor.getValue();
        assertEquals("+Alien*", query.getFullText());
        assertEquals(20, query.getPageSize()); assertEquals(40, query.getOffset());
        assertEquals("rating", query.getSort1()); assertEquals("title", query.getSort2());
        assertEquals("DESC", query.getDir1()); assertEquals("ASC", query.getDir2());
    }

    private SearchService service(AutocompleteRepository autocomplete, MovieListRepository movies) {
        return new SearchService(new FullTextQueryBuilder(), autocomplete, movies);
    }
}
