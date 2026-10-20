package com.fabflix.boot.search;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutocompleteRepositoryTest {
    private static final String EXPECTED_SQL = "SELECT id, title FROM movies "
            + "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) LIMIT 10";

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsIdAndTitleIntoLegacySuggestionShape() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("id")).thenReturn("tt0001");
        when(resultSet.getString("title")).thenReturn("Example Movie");
        when(jdbcTemplate.query(eq(EXPECTED_SQL), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        List<AutocompleteSuggestion> suggestions =
                new AutocompleteRepository(jdbcTemplate).findSuggestions("exa*");

        assertEquals(1, suggestions.size());
        assertEquals("Example Movie", suggestions.get(0).getValue());
        assertEquals("tt0001", suggestions.get(0).getData().get("movie_id"));
        verify(jdbcTemplate).query(eq(EXPECTED_SQL), any(RowMapper.class), eq("exa*"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyListWhenNoTitleMatches() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(eq(EXPECTED_SQL), any(RowMapper.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        List<AutocompleteSuggestion> suggestions =
                new AutocompleteRepository(jdbcTemplate).findSuggestions("zzz*");

        assertTrue(suggestions.isEmpty());
    }
}
