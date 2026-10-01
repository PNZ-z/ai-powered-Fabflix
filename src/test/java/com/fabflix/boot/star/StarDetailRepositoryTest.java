package com.fabflix.boot.star;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarDetailRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyWithoutQueryingMoviesWhenStarDoesNotExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(
                eq(StarDetailRepository.STAR_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(Collections.emptyList());

        Optional<StarDetail> result = new StarDetailRepository(jdbcTemplate).findById("missing-id");

        assertTrue(result.isEmpty());
        verify(jdbcTemplate, never()).query(
                eq(StarDetailRepository.MOVIES_SQL),
                any(RowMapper.class),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsStarAndMoviesUsingBoundStarId() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet starResult = mock(ResultSet.class);
        ResultSet movieResult = mock(ResultSet.class);

        when(starResult.getString("id")).thenReturn("nm0000244");
        when(starResult.getString("name")).thenReturn("Sigourney Weaver");
        when(starResult.getInt("birth_year")).thenReturn(1949);
        when(starResult.wasNull()).thenReturn(false);
        when(movieResult.getString("id")).thenReturn("tt0078748");
        when(movieResult.getString("title")).thenReturn("Alien");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    RowMapper mapper = invocation.getArgument(1);
                    return sql.equals(StarDetailRepository.STAR_SQL)
                            ? List.of(mapper.mapRow(starResult, 0))
                            : List.of(mapper.mapRow(movieResult, 0));
                });

        StarDetail star = new StarDetailRepository(jdbcTemplate)
                .findById("nm0000244")
                .orElseThrow();

        assertEquals("nm0000244", star.getStarId());
        assertEquals("Sigourney Weaver", star.getStarName());
        assertEquals(1949, star.getStarBirthYear());
        assertEquals("tt0078748", star.getStarMovies().get(0).getMovieId());
        assertEquals("Alien", star.getStarMovies().get(0).getMovieTitle());
        verify(jdbcTemplate).query(
                eq(StarDetailRepository.STAR_SQL),
                any(RowMapper.class),
                eq("nm0000244")
        );
        verify(jdbcTemplate).query(
                eq(StarDetailRepository.MOVIES_SQL),
                any(RowMapper.class),
                eq("nm0000244")
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void preservesLegacyFallbackForMissingBirthYearAndAllowsNoMovies() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet starResult = mock(ResultSet.class);
        when(starResult.getString("id")).thenReturn("nm0000001");
        when(starResult.getString("name")).thenReturn("Unknown Birth Year");
        when(starResult.getInt("birth_year")).thenReturn(0);
        when(starResult.wasNull()).thenReturn(true);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.equals(StarDetailRepository.MOVIES_SQL)) {
                        return Collections.emptyList();
                    }
                    RowMapper mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(starResult, 0));
                });

        StarDetail star = new StarDetailRepository(jdbcTemplate)
                .findById("nm0000001")
                .orElseThrow();

        assertEquals("N/A", star.getStarBirthYear());
        assertTrue(star.getStarMovies().isEmpty());
    }
}
