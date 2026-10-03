package com.fabflix.boot.commerce;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartRepositoryTest {
    @Test
    void skipsDatabaseForEmptyCart() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        assertTrue(new CartRepository(jdbcTemplate).findMoviesByIds(List.of()).isEmpty());
        verify(jdbcTemplate, never()).query(
                org.mockito.ArgumentMatchers.anyString(),
                any(RowMapper.class),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loadsAllCartMoviesWithBoundPlaceholders() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("id")).thenReturn("tt0001");
        when(resultSet.getString("title")).thenReturn("Example");
        when(jdbcTemplate.query(
                eq("SELECT id, title FROM movies WHERE id IN (?, ?)"),
                any(RowMapper.class),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        Map<String, MovieCartSummary> movies = new CartRepository(jdbcTemplate)
                .findMoviesByIds(List.of("tt0001", "tt0002"));

        assertEquals("Example", movies.get("tt0001").getTitle());
        verify(jdbcTemplate).query(
                eq("SELECT id, title FROM movies WHERE id IN (?, ?)"),
                any(RowMapper.class),
                eq("tt0001"),
                eq("tt0002")
        );
    }
}
