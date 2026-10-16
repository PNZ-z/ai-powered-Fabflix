package com.fabflix.boot.search;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.fabflix.boot.movie.MovieSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final SearchService searchService;

    public SearchController(SearchService searchService) { this.searchService = searchService; }

    @GetMapping("/api/boot/autocomplete")
    public List<AutocompleteSuggestion> autocomplete(
            @RequestParam(value = "query", required = false) String query) {
        List<AutocompleteSuggestion> results = searchService.autocomplete(query);
        logger.info("Autocomplete request completed: resultCount={}", results.size());
        return results;
    }

    @GetMapping("/api/boot/full-text-search")
    public List<MovieSummary> fullTextSearch(@ModelAttribute SearchRequest request,
            HttpServletRequest servletRequest, HttpSession session) {
        List<MovieSummary> results = searchService.search(request);
        if (!results.isEmpty()) session.setAttribute("lastMovieQuery", servletRequest.getQueryString());
        logger.info("Full-text search completed: resultCount={}", results.size());
        return results;
    }

    @GetMapping("/api/boot/jump-back")
    public ResponseEntity<Void> jumpBack(HttpSession session) {
        Object stored = session.getAttribute("lastMovieQuery");
        String query = stored instanceof String ? ((String) stored).trim() : "";
        if (query.isEmpty()) query = "title=&pageSize=10&pageNumber=0";
        // mode=fulltext is required: the React list page only stores/restores the
        // last query for full-text searches, so the browse mode would drop it.
        return ResponseEntity.status(302).location(URI.create("/movies?mode=fulltext&" + query)).build();
    }
}
