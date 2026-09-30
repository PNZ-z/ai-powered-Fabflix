package com.fabflix.boot.movie;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MovieDetailRepository {
    private static final Logger logger = LoggerFactory.getLogger(MovieDetailRepository.class);

    static final String MOVIE_SQL =
            "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON r.movie_id = m.id " +
                    "LEFT JOIN genres_in_movies gim ON gim.movie_id = m.id " +
                    "LEFT JOIN genres g ON g.id = gim.genre_id " +
                    "WHERE m.id = ? " +
                    "GROUP BY m.id, m.title, m.year, m.director, r.rating";

    static final String STARS_SQL =
            "SELECT s.id, s.name " +
                    "FROM stars_in_movies selected_movie " +
                    "JOIN stars s ON s.id = selected_movie.star_id " +
                    "LEFT JOIN stars_in_movies all_movies ON all_movies.star_id = s.id " +
                    "WHERE selected_movie.movie_id = ? " +
                    "GROUP BY s.id, s.name " +
                    "ORDER BY COUNT(all_movies.movie_id) DESC, s.name ASC, s.id ASC";

    private final JdbcTemplate jdbcTemplate;

    public MovieDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MovieDetail> findById(String movieId) {
        long startedAt = System.nanoTime();
        List<MovieRow> movies = jdbcTemplate.query(MOVIE_SQL, this::mapMovieRow, movieId);

        if (movies.isEmpty()) {
            logger.debug("Movie detail query found no result: movieId={}", movieId);
            return Optional.empty();
        }

        MovieRow movie = movies.get(0);
        List<MovieDetailStar> stars = jdbcTemplate.query(
                STARS_SQL,
                (resultSet, rowNumber) -> new MovieDetailStar(
                        resultSet.getString("id"),
                        resultSet.getString("name")
                ),
                movieId
        );
        if (stars.isEmpty()) {
            stars = List.of(new MovieDetailStar("", "N/A"));
        }

        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        logger.debug("Movie detail query completed: movieId={}, durationMs={}", movieId, durationMillis);
        return Optional.of(new MovieDetail(
                movie.id,
                movie.title,
                movie.year,
                movie.director,
                movie.genres,
                movie.rating,
                stars
        ));
    }

    private MovieRow mapMovieRow(ResultSet resultSet, int rowNumber) throws SQLException {
        String genres = resultSet.getString("genres");
        float ratingValue = resultSet.getFloat("rating");
        String rating = resultSet.wasNull() ? "N/A" : String.valueOf(ratingValue);

        return new MovieRow(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getInt("year"),
                resultSet.getString("director"),
                genres == null ? "" : genres,
                rating
        );
    }

    private static class MovieRow {
        private final String id;
        private final String title;
        private final int year;
        private final String director;
        private final String genres;
        private final String rating;

        private MovieRow(
                String id,
                String title,
                int year,
                String director,
                String genres,
                String rating) {
            this.id = id;
            this.title = title;
            this.year = year;
            this.director = director;
            this.genres = genres;
            this.rating = rating;
        }
    }
}
