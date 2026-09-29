package com.fabflix.boot.movie;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MovieListRepositoryTest {
    @Test
    void buildsDefaultMovieListSql() {
        MovieListQuery query = new MovieListQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                10,
                0,
                "title",
                "ASC",
                "rating",
                "ASC"
        );
        MovieListRepository repository = new MovieListRepository(mock(JdbcTemplate.class));

        String sql = repository.buildSql(query);

        assertTrue(sql.contains("WITH FilteredMovies AS"));
        assertTrue(sql.contains("PagedMovies AS"));
        assertTrue(sql.contains("PagedMovieStars AS"));
        assertTrue(sql.contains("RelevantStarCounts AS"));
        assertTrue(sql.contains("RankedStars AS"));
        assertTrue(sql.contains("TopStars AS"));
        assertTrue(sql.contains("MovieGenres AS"));
        assertTrue(sql.contains(
                "ORDER BY fm.title ASC, fm.rating IS NULL, fm.rating ASC, fm.id ASC " +
                        "LIMIT ? OFFSET ?"));
        assertTrue(sql.endsWith(
                "ORDER BY pm.title ASC, pm.rating IS NULL, pm.rating ASC, pm.id ASC"));
        assertFalse(sql.contains("sim_filter"));
        assertFalse(sql.contains("gim_filter"));
        assertEquals(2, countPlaceholders(sql));
    }

    @Test
    void addsSearchFiltersOnlyWhenPresent() {
        MovieListQuery query = new MovieListQuery(
                "Alien",
                "1979",
                "Scott",
                "Weaver",
                "Horror",
                "A",
                25,
                50,
                "rating",
                "DESC",
                "title",
                "ASC"
        );
        MovieListRepository repository = new MovieListRepository(mock(JdbcTemplate.class));

        String sql = repository.buildSql(query);

        assertTrue(sql.contains("AND m.title LIKE ?"));
        assertTrue(sql.contains("AND m.year = ?"));
        assertTrue(sql.contains("AND m.director LIKE ?"));
        assertTrue(sql.contains("FROM stars_in_movies sim_filter"));
        assertTrue(sql.contains(
                "WHERE sim_filter.movie_id = m.id AND s_filter.name LIKE ?"));
        assertTrue(sql.contains("FROM genres_in_movies gim_filter"));
        assertTrue(sql.contains(
                "WHERE gim_filter.movie_id = m.id AND g_filter.name = ?"));
        assertTrue(sql.contains("AND UPPER(m.title) LIKE CONCAT(UPPER(?), '%')"));
        assertTrue(sql.contains(
                "ORDER BY fm.rating IS NULL, fm.rating DESC, fm.title ASC, fm.id ASC " +
                        "LIMIT ? OFFSET ?"));
        assertEquals(8, countPlaceholders(sql));

        MovieListQuery unsafeSortQuery = new MovieListQuery(
                null,
                null,
                null,
                null,
                null,
                null,
                10,
                0,
                "title; DROP TABLE movies",
                "DESC; DROP TABLE movies",
                "rating",
                "DESC"
        );

        String safeSql = repository.buildSql(unsafeSortQuery);

        assertFalse(safeSql.contains("DROP TABLE"));
        assertTrue(safeSql.contains(
                "ORDER BY fm.title ASC, fm.rating IS NULL, fm.rating DESC, fm.id ASC"));
    }

    @Test
    void mapsDatabaseRowsToMovieSummary() throws Exception {
        MovieListRepository repository = new MovieListRepository(mock(JdbcTemplate.class));
        ResultSet populatedRow = mock(ResultSet.class);
        when(populatedRow.getString("id")).thenReturn("tt0078748");
        when(populatedRow.getString("title")).thenReturn("Alien");
        when(populatedRow.getInt("year")).thenReturn(1979);
        when(populatedRow.getString("director")).thenReturn("Ridley Scott");
        when(populatedRow.getString("genres")).thenReturn("Horror, Sci-Fi");
        when(populatedRow.getFloat("rating")).thenReturn(8.5F);
        when(populatedRow.wasNull()).thenReturn(false);
        when(populatedRow.getString("star_ids")).thenReturn("nm0000244,nm0000648");
        when(populatedRow.getString("star_names")).thenReturn("Sigourney Weaver\tJohn Hurt");

        MovieSummary movie = repository.mapMovieSummary(populatedRow, 0);

        assertEquals("tt0078748", movie.getMovieId());
        assertEquals("Alien", movie.getMovieTitle());
        assertEquals(1979, movie.getMovieYear());
        assertEquals("Ridley Scott", movie.getMovieDirector());
        assertEquals("Horror, Sci-Fi", movie.getMovieGenres());
        assertEquals(8.5F, movie.getMovieRating());
        assertEquals(2, movie.getMovieStars().size());
        assertEquals("nm0000244", movie.getMovieStars().get(0).getStarId());
        assertEquals("Sigourney Weaver", movie.getMovieStars().get(0).getStarName());
        assertEquals("nm0000648", movie.getMovieStars().get(1).getStarId());
        assertEquals("John Hurt", movie.getMovieStars().get(1).getStarName());

        ResultSet emptyOptionalValuesRow = mock(ResultSet.class);
        when(emptyOptionalValuesRow.getString("id")).thenReturn("tt-no-details");
        when(emptyOptionalValuesRow.getString("title")).thenReturn("Unknown");
        when(emptyOptionalValuesRow.getInt("year")).thenReturn(2000);
        when(emptyOptionalValuesRow.getString("director")).thenReturn("Unknown");
        when(emptyOptionalValuesRow.getString("genres")).thenReturn(null);
        when(emptyOptionalValuesRow.getFloat("rating")).thenReturn(0.0F);
        when(emptyOptionalValuesRow.wasNull()).thenReturn(true);
        when(emptyOptionalValuesRow.getString("star_ids")).thenReturn(null);
        when(emptyOptionalValuesRow.getString("star_names")).thenReturn(null);

        MovieSummary movieWithoutOptionalValues =
                repository.mapMovieSummary(emptyOptionalValuesRow, 1);

        assertEquals("", movieWithoutOptionalValues.getMovieGenres());
        assertNull(movieWithoutOptionalValues.getMovieRating());
        assertEquals(1, movieWithoutOptionalValues.getMovieStars().size());
        assertEquals("", movieWithoutOptionalValues.getMovieStars().get(0).getStarId());
        assertEquals("N/A", movieWithoutOptionalValues.getMovieStars().get(0).getStarName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void bindsQueryParametersInPlaceholderOrder() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        MovieListRepository repository = new MovieListRepository(jdbcTemplate);
        MovieListQuery query = new MovieListQuery(
                "Alien",
                "1979",
                "Scott",
                "Weaver",
                "Horror",
                "A",
                10,
                20,
                "title",
                "ASC",
                "rating",
                "DESC"
        );
        List<MovieSummary> expectedMovies = List.of(new MovieSummary());
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(expectedMovies);

        List<MovieSummary> actualMovies = repository.findMovies(query);

        assertSame(expectedMovies, actualMovies);

        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(
                eq(repository.buildSql(query)),
                any(RowMapper.class),
                parameters.capture()
        );
        assertEquals(
                List.of("%Alien%", "1979", "%Scott%", "%Weaver%", "Horror", "A", 11, 20),
                List.of(parameters.getValue())
        );
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }
}
