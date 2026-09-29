package com.fabflix.boot.movie;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MovieListRepository {
    private static final Logger logger = LoggerFactory.getLogger(MovieListRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public MovieListRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    public List<MovieSummary> findMovies(MovieListQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        String sql = buildSql(query);
        List<Object> parameters = new ArrayList<>();

        if (query.getTitle() != null) {
            parameters.add("%" + query.getTitle() + "%");
        }
        if (query.getYear() != null) {
            parameters.add(query.getYear());
        }
        if (query.getDirector() != null) {
            parameters.add("%" + query.getDirector() + "%");
        }
        if (query.getStar() != null) {
            parameters.add("%" + query.getStar() + "%");
        }
        if (query.getGenre() != null) {
            parameters.add(query.getGenre());
        }
        if (query.getStartsWith() != null) {
            parameters.add(query.getStartsWith());
        }

        parameters.add(query.getPageSize() + 1);
        parameters.add(query.getOffset());

        logger.debug(
                "Executing movie list query: pageSize={}, offset={}",
                query.getPageSize(),
                query.getOffset()
        );
        long startedAt = System.nanoTime();
        List<MovieSummary> movies = jdbcTemplate.query(
                sql,
                this::mapMovieSummary,
                parameters.toArray()
        );
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        logger.debug(
                "Movie list query completed: resultCount={}, durationMs={}",
                movies.size(),
                durationMillis
        );

        return movies;
    }

    String buildSql(MovieListQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        String filteredOrder = buildOrderBy(query, "fm");
        String pagedOrder = buildOrderBy(query, "pm");
        StringBuilder sql = new StringBuilder(
                "WITH FilteredMovies AS ( " +
                        "SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movie_id " +
                        "WHERE 1 = 1 ");

        if (query.getTitle() != null) {
            sql.append("AND m.title LIKE ? ");
        }
        if (query.getYear() != null) {
            sql.append("AND m.year = ? ");
        }
        if (query.getDirector() != null) {
            sql.append("AND m.director LIKE ? ");
        }
        if (query.getStar() != null) {
            sql.append(
                    "AND EXISTS ( " +
                            "SELECT 1 " +
                            "FROM stars_in_movies sim_filter " +
                            "JOIN stars s_filter ON s_filter.id = sim_filter.star_id " +
                            "WHERE sim_filter.movie_id = m.id AND s_filter.name LIKE ? " +
                            ") ");
        }
        if (query.getGenre() != null) {
            sql.append(
                    "AND EXISTS ( " +
                            "SELECT 1 " +
                            "FROM genres_in_movies gim_filter " +
                            "JOIN genres g_filter ON g_filter.id = gim_filter.genre_id " +
                            "WHERE gim_filter.movie_id = m.id AND g_filter.name = ? " +
                            ") ");
        }
        if (query.getStartsWith() != null) {
            sql.append("AND UPPER(m.title) LIKE CONCAT(UPPER(?), '%') ");
        }

        sql.append(
                "), " +
                        "PagedMovies AS ( " +
                        "SELECT fm.id, fm.title, fm.year, fm.director, fm.rating " +
                        "FROM FilteredMovies fm " +
                        "ORDER BY " + filteredOrder + " " +
                        "LIMIT ? OFFSET ? " +
                        "), " +
                        "PagedMovieStars AS ( " +
                        "SELECT sim.movie_id, sim.star_id " +
                        "FROM stars_in_movies sim " +
                        "JOIN PagedMovies pm ON pm.id = sim.movie_id " +
                        "), " +
                        "RelevantStarCounts AS ( " +
                        "SELECT sim.star_id, COUNT(*) AS movie_count " +
                        "FROM stars_in_movies sim " +
                        "JOIN (SELECT DISTINCT star_id FROM PagedMovieStars) relevant_stars " +
                        "ON relevant_stars.star_id = sim.star_id " +
                        "GROUP BY sim.star_id " +
                        "), " +
                        "RankedStars AS ( " +
                        "SELECT pms.movie_id, s.id AS star_id, s.name AS star_name, " +
                        "ROW_NUMBER() OVER ( " +
                        "PARTITION BY pms.movie_id " +
                        "ORDER BY rsc.movie_count DESC, s.name ASC, s.id ASC " +
                        ") AS star_rank " +
                        "FROM PagedMovieStars pms " +
                        "JOIN stars s ON s.id = pms.star_id " +
                        "JOIN RelevantStarCounts rsc ON rsc.star_id = pms.star_id " +
                        "), " +
                        "TopStars AS ( " +
                        "SELECT movie_id, " +
                        "GROUP_CONCAT(star_id ORDER BY star_rank SEPARATOR ',') AS star_ids, " +
                        "GROUP_CONCAT(star_name ORDER BY star_rank SEPARATOR '\\t') AS star_names " +
                        "FROM RankedStars " +
                        "WHERE star_rank <= 3 " +
                        "GROUP BY movie_id " +
                        "), " +
                        "MovieGenres AS ( " +
                        "SELECT pm.id AS movie_id, " +
                        "SUBSTRING_INDEX( " +
                        "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), " +
                        "', ', 3 " +
                        ") AS genres " +
                        "FROM PagedMovies pm " +
                        "LEFT JOIN genres_in_movies gim ON gim.movie_id = pm.id " +
                        "LEFT JOIN genres g ON g.id = gim.genre_id " +
                        "GROUP BY pm.id " +
                        ") " +
                        "SELECT pm.id, pm.title, pm.year, pm.director, pm.rating, " +
                        "mg.genres, ts.star_ids, ts.star_names " +
                        "FROM PagedMovies pm " +
                        "LEFT JOIN MovieGenres mg ON mg.movie_id = pm.id " +
                        "LEFT JOIN TopStars ts ON ts.movie_id = pm.id " +
                        "ORDER BY " + pagedOrder);

        return sql.toString();
    }

    private String buildOrderBy(MovieListQuery query, String alias) {
        String sort1 = sortColumn(query.getSort1(), alias);
        String sort2 = sortColumn(query.getSort2(), alias);
        String dir1 = sortDirection(query.getDir1());
        String dir2 = sortDirection(query.getDir2());
        StringBuilder orderBy = new StringBuilder();

        appendSort(orderBy, sort1, dir1, alias);
        orderBy.append(", ");
        appendSort(orderBy, sort2, dir2, alias);
        orderBy.append(", ").append(alias).append(".id ASC");

        return orderBy.toString();
    }

    private void appendSort(
            StringBuilder orderBy, String column, String direction, String alias) {
        if (column.equals(alias + ".rating")) {
            orderBy.append(alias).append(".rating IS NULL, ");
        }
        orderBy.append(column).append(" ").append(direction);
    }

    private String sortColumn(String field, String alias) {
        return "rating".equalsIgnoreCase(field)
                ? alias + ".rating"
                : alias + ".title";
    }

    private String sortDirection(String direction) {
        return "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
    }

    MovieSummary mapMovieSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        String genres = resultSet.getString("genres");
        if (genres == null) {
            genres = "";
        }

        float ratingValue = resultSet.getFloat("rating");
        Float rating = resultSet.wasNull() ? null : ratingValue;
        List<MovieSummaryStar> stars = new ArrayList<>();
        String starIds = resultSet.getString("star_ids");
        String starNames = resultSet.getString("star_names");

        if (starIds != null && !starIds.isEmpty()
                && starNames != null && !starNames.isEmpty()) {
            String[] starIdValues = starIds.split(",");
            String[] starNameValues = starNames.split("\t");
            int starCount = Math.min(starIdValues.length, starNameValues.length);

            for (int i = 0; i < starCount; i++) {
                stars.add(new MovieSummaryStar(starIdValues[i], starNameValues[i]));
            }
        }

        if (stars.isEmpty()) {
            stars.add(new MovieSummaryStar("", "N/A"));
        }

        return new MovieSummary(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getInt("year"),
                resultSet.getString("director"),
                genres,
                rating,
                stars
        );
    }
}
