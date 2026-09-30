package com.fabflix.boot.movie;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovieDetailRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyWithoutQueryingStarsWhenMovieDoesNotExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(
                eq(MovieDetailRepository.MOVIE_SQL),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(Collections.emptyList());

        Optional<MovieDetail> result = new MovieDetailRepository(jdbcTemplate).findById("missing-id");

        assertTrue(result.isEmpty());
        verify(jdbcTemplate, never()).query(
                eq(MovieDetailRepository.STARS_SQL),
                any(RowMapper.class),
                any(Object[].class)
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void mapsMovieAndStarsUsingBoundMovieId() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet movieResult = mock(ResultSet.class);
        ResultSet starResult = mock(ResultSet.class);

        when(movieResult.getString("id")).thenReturn("tt0078748");
        when(movieResult.getString("title")).thenReturn("Alien");
        when(movieResult.getInt("year")).thenReturn(1979);
        when(movieResult.getString("director")).thenReturn("Ridley Scott");
        when(movieResult.getString("genres")).thenReturn("Horror, Sci-Fi");
        when(movieResult.getFloat("rating")).thenReturn(8.5F);
        when(movieResult.wasNull()).thenReturn(false);
        when(starResult.getString("id")).thenReturn("nm0000244");
        when(starResult.getString("name")).thenReturn("Sigourney Weaver");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    RowMapper mapper = invocation.getArgument(1);
                    return sql.equals(MovieDetailRepository.MOVIE_SQL)
                            ? List.of(mapper.mapRow(movieResult, 0))
                            : List.of(mapper.mapRow(starResult, 0));
                });

        Optional<MovieDetail> result = new MovieDetailRepository(jdbcTemplate).findById("tt0078748");

        assertTrue(result.isPresent());
        MovieDetail movie = result.get();
        assertEquals("tt0078748", movie.getMovieId());
        assertEquals("Alien", movie.getMovieTitle());
        assertEquals(1979, movie.getMovieYear());
        assertEquals("Ridley Scott", movie.getMovieDirector());
        assertEquals("Horror, Sci-Fi", movie.getMovieGenres());
        assertEquals("8.5", movie.getMovieRating());
        assertEquals("nm0000244", movie.getMovieStars().get(0).getStarId());
        assertEquals("Sigourney Weaver", movie.getMovieStars().get(0).getStarName());
        verify(jdbcTemplate).query(
                eq(MovieDetailRepository.MOVIE_SQL),
                any(RowMapper.class),
                eq("tt0078748")
        );
        verify(jdbcTemplate).query(
                eq(MovieDetailRepository.STARS_SQL),
                any(RowMapper.class),
                eq("tt0078748")
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void preservesLegacyFallbacksForNullValuesAndMissingStars() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet movieResult = mock(ResultSet.class);
        when(movieResult.getString("id")).thenReturn("tt0001");
        when(movieResult.getString("title")).thenReturn("Example Movie");
        when(movieResult.getInt("year")).thenReturn(2001);
        when(movieResult.getString("director")).thenReturn("Example Director");
        when(movieResult.getString("genres")).thenReturn(null);
        when(movieResult.getFloat("rating")).thenReturn(0.0F);
        when(movieResult.wasNull()).thenReturn(true);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.equals(MovieDetailRepository.STARS_SQL)) {
                        return Collections.emptyList();
                    }
                    RowMapper mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(movieResult, 0));
                });

        MovieDetail movie = new MovieDetailRepository(jdbcTemplate)
                .findById("tt0001")
                .orElseThrow();

        assertEquals("", movie.getMovieGenres());
        assertEquals("N/A", movie.getMovieRating());
        assertFalse(movie.getMovieStars().isEmpty());
        assertEquals("", movie.getMovieStars().get(0).getStarId());
        assertEquals("N/A", movie.getMovieStars().get(0).getStarName());
    }
}
