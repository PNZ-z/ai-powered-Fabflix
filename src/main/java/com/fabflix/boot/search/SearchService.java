package com.fabflix.boot.search;

import java.util.Collections;
import java.util.List;

import com.fabflix.boot.movie.MovieListQuery;
import com.fabflix.boot.movie.MovieListRepository;
import com.fabflix.boot.movie.MovieSummary;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final FullTextQueryBuilder queryBuilder;
    private final AutocompleteRepository autocompleteRepository;
    private final MovieListRepository movieRepository;

    public SearchService(FullTextQueryBuilder queryBuilder,
            AutocompleteRepository autocompleteRepository, MovieListRepository movieRepository) {
        this.queryBuilder = queryBuilder;
        this.autocompleteRepository = autocompleteRepository;
        this.movieRepository = movieRepository;
    }

    public List<AutocompleteSuggestion> autocomplete(String input) {
        if (input == null || input.trim().length() < 3) {
            return Collections.emptyList();
        }
        String query = queryBuilder.build(input);
        return query.isEmpty() ? Collections.emptyList() : autocompleteRepository.findSuggestions(query);
    }

    public List<MovieSummary> search(SearchRequest request) {
        String fullText = queryBuilder.build(request.getQuery());
        if (fullText.isEmpty()) {
            return Collections.emptyList();
        }
        int pageSize = positiveInteger(request.getPageSize(), 10);
        int pageNumber = nonNegativeInteger(request.getPageNumber(), 0);
        String sort1 = sortField(request.getSort1(), "title");
        String sort2 = sortField(request.getSort2(), "rating");
        if (sort1.equals(sort2)) sort2 = sort1.equals("title") ? "rating" : "title";
        long requestedOffset = (long) pageNumber * pageSize;
        int offset = requestedOffset <= Integer.MAX_VALUE ? (int) requestedOffset : 0;
        MovieListQuery query = new MovieListQuery(null, null, null, null, null, null,
                pageSize, offset, sort1,
                direction(request.getDir1()), sort2, direction(request.getDir2()), fullText);
        return movieRepository.findMovies(query);
    }

    private int positiveInteger(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
    private int nonNegativeInteger(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed >= 0 ? parsed : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
    private String sortField(String value, String fallback) {
        if ("title".equalsIgnoreCase(value)) return "title";
        if ("rating".equalsIgnoreCase(value)) return "rating";
        return fallback;
    }
    private String direction(String value) {
        return "DESC".equalsIgnoreCase(value) ? "DESC" : "ASC";
    }
}
