package com.fabflix.boot.commerce;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {
    private final JdbcTemplate jdbcTemplate;

    public CartRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, MovieCartSummary> findMoviesByIds(Collection<String> movieIds) {
        if (movieIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(", ", Collections.nCopies(movieIds.size(), "?"));
        String sql = "SELECT id, title FROM movies WHERE id IN (" + placeholders + ")";
        List<MovieCartSummary> movies = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> new MovieCartSummary(
                        resultSet.getString("id"),
                        resultSet.getString("title")
                ),
                movieIds.toArray()
        );
        return movies.stream().collect(Collectors.toMap(
                MovieCartSummary::getId,
                Function.identity()
        ));
    }
}
