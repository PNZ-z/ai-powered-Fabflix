package com.fabflix.boot.search;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AutocompleteRepository {
    private static final String SQL = "SELECT id, title FROM movies "
            + "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) LIMIT 10";
    private final JdbcTemplate jdbcTemplate;

    public AutocompleteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AutocompleteSuggestion> findSuggestions(String fullTextQuery) {
        return jdbcTemplate.query(SQL,
                (rs, rowNumber) -> new AutocompleteSuggestion(
                        rs.getString("id"), rs.getString("title")),
                fullTextQuery);
    }
}
