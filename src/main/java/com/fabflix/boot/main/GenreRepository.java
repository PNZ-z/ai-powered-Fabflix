package com.fabflix.boot.main;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GenreRepository {
    private final JdbcTemplate jdbcTemplate;

    public GenreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Genre> findAllSortedByName() {
        return jdbcTemplate.query(
                "SELECT name FROM genres ORDER BY name ASC",
                (resultSet, rowNumber) -> new Genre(resultSet.getString("name"))
        );
    }
}
