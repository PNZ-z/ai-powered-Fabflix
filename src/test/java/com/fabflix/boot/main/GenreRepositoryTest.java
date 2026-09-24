package com.fabflix.boot.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@SuppressWarnings("unchecked")
class GenreRepositoryTest {
    private final JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
    private final GenreRepository genreRepository = new GenreRepository(jdbcTemplate);

    @Test
    void queriesGenresInAlphabeticalOrder() {
        when(jdbcTemplate.query(eq("SELECT name FROM genres ORDER BY name ASC"), any(RowMapper.class)))
                .thenReturn(List.of(new Genre("Action"), new Genre("Comedy")));

        List<Genre> genres = genreRepository.findAllSortedByName();

        assertEquals(2, genres.size());
        assertEquals("Action", genres.get(0).getName());
        assertEquals("Comedy", genres.get(1).getName());
    }

    @Test
    void mapsRowsToGenre() throws Exception {
        ArgumentCaptor<RowMapper<Genre>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(eq("SELECT name FROM genres ORDER BY name ASC"), mapperCaptor.capture()))
                .thenReturn(List.of());

        genreRepository.findAllSortedByName();

        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getString("name")).thenReturn("Drama");

        Genre genre = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals("Drama", genre.getName());
        verify(resultSet).getString("name");
    }
}
