package com.fabflix.boot.star;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StarDetailRepository {
    private static final Logger logger = LoggerFactory.getLogger(StarDetailRepository.class);

    static final String STAR_SQL =
            "SELECT s.id, s.name, s.birth_year " +
                    "FROM stars s " +
                    "WHERE s.id = ?";

    static final String MOVIES_SQL =
            "SELECT m.id, m.title " +
                    "FROM stars_in_movies sim " +
                    "JOIN movies m ON m.id = sim.movie_id " +
                    "WHERE sim.star_id = ? " +
                    "ORDER BY m.year DESC, m.title ASC, m.id ASC";

    private final JdbcTemplate jdbcTemplate;

    public StarDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StarDetail> findById(String starId) {
        long startedAt = System.nanoTime();
        List<StarRow> stars = jdbcTemplate.query(STAR_SQL, this::mapStarRow, starId);
        if (stars.isEmpty()) {
            logger.debug("Star detail query found no result: starId={}", starId);
            return Optional.empty();
        }

        StarRow star = stars.get(0);
        List<StarDetailMovie> movies = jdbcTemplate.query(
                MOVIES_SQL,
                (resultSet, rowNumber) -> new StarDetailMovie(
                        resultSet.getString("id"),
                        resultSet.getString("title")
                ),
                starId
        );

        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        logger.debug("Star detail query completed: starId={}, movieCount={}, durationMs={}",
                starId, movies.size(), durationMillis);
        return Optional.of(new StarDetail(star.id, star.name, star.birthYear, movies));
    }

    private StarRow mapStarRow(ResultSet resultSet, int rowNumber) throws SQLException {
        int birthYear = resultSet.getInt("birth_year");
        return new StarRow(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.wasNull() ? "N/A" : birthYear
        );
    }

    private static class StarRow {
        private final String id;
        private final String name;
        private final Object birthYear;

        private StarRow(String id, String name, Object birthYear) {
            this.id = id;
            this.name = name;
            this.birthYear = birthYear;
        }
    }
}
