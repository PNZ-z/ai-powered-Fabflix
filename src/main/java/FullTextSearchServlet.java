import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/api/full-text-search")
public class FullTextSearchServlet extends HttpServlet {
    private DataSource dataSource;
    private final ConcurrentHashMap<String, String> queryCache = new ConcurrentHashMap<>();

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private String getTrimmedParameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? null : value.trim();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String cacheKey = request.getQueryString();

        if (cacheKey != null && queryCache.containsKey(cacheKey)) {
            out.write(queryCache.get(cacheKey));
            out.close();
            return;
        }

        String query = getTrimmedParameter(request, "query");
        String searchPageNumber = getTrimmedParameter(request, "pageNumber");
        String pageSizeParam = getTrimmedParameter(request, "pageSize");

        int pageSize = 10;
        if (pageSizeParam != null && !pageSizeParam.isEmpty() && isInteger(pageSizeParam)) {
            pageSize = Integer.parseInt(pageSizeParam);
        }

        int offset = 0;
        if (searchPageNumber != null && !searchPageNumber.isEmpty() && isInteger(searchPageNumber)) {
            offset = Integer.parseInt(searchPageNumber) * pageSize;
        }

        String sort1 = request.getParameter("sort1");
        String dir1 = request.getParameter("dir1");
        String sort2 = request.getParameter("sort2");
        String dir2 = request.getParameter("dir2");

        if (sort1 == null) sort1 = "title";
        if (sort2 == null) sort2 = "rating";

        dir1 = "DESC".equalsIgnoreCase(dir1) ? "DESC" : "ASC";
        dir2 = "DESC".equalsIgnoreCase(dir2) ? "DESC" : "ASC";

        String col1 = "rating".equalsIgnoreCase(sort1) ? "fm.rating" : "fm.title";
        String col2 = "rating".equalsIgnoreCase(sort2) ? "fm.rating" : "fm.title";

        String fullTextQuery = buildFullTextQuery(query);

        if (fullTextQuery.isEmpty()) {
            out.write("[]");
            out.close();
            return;
        }

        String movieQuery =
                "WITH FilteredMovies AS ( " +
                        "SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating " +
                        "FROM movies m " +
                        "LEFT JOIN ratings r ON m.id = r.movie_id " +
                        "WHERE MATCH(m.title) AGAINST(? IN BOOLEAN MODE) " +
                        "), " +
                        "StarMovieCounts AS ( " +
                        "   SELECT star_id, COUNT(*) AS movie_count " +
                        "   FROM stars_in_movies " +
                        "   GROUP BY star_id " +
                        "), " +
                        "RankedStars AS ( " +
                        "   SELECT " +
                        "       sim.movie_id, " +
                        "       s.id AS star_id, " +
                        "       s.name AS star_name, " +
                        "       smc.movie_count, " +
                        "       ROW_NUMBER() OVER ( " +
                        "           PARTITION BY sim.movie_id " +
                        "           ORDER BY smc.movie_count DESC, s.name ASC, s.id ASC " +
                        "       ) AS star_rank " +
                        "   FROM stars_in_movies sim " +
                        "   JOIN FilteredMovies fm ON sim.movie_id = fm.id " +
                        "   JOIN stars s ON sim.star_id = s.id " +
                        "   JOIN StarMovieCounts smc ON sim.star_id = smc.star_id " +
                        "), " +
                        "TopStars AS ( " +
                        "   SELECT " +
                        "       movie_id, " +
                        "       GROUP_CONCAT(star_id ORDER BY star_rank SEPARATOR ',') AS star_ids, " +
                        "       GROUP_CONCAT(star_name ORDER BY star_rank SEPARATOR '\\t') AS star_names " +
                        "   FROM RankedStars " +
                        "   WHERE star_rank <= 3 " +
                        "   GROUP BY movie_id " +
                        "), " +
                        "MovieGenres AS ( " +
                        "   SELECT " +
                        "       fm.id AS movie_id, " +
                        "       SUBSTRING_INDEX( " +
                        "           GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), " +
                        "           ', ', " +
                        "           3 " +
                        "       ) AS genres " +
                        "   FROM FilteredMovies fm " +
                        "   LEFT JOIN genres_in_movies gim ON fm.id = gim.movie_id " +
                        "   LEFT JOIN genres g ON gim.genre_id = g.id " +
                        "   GROUP BY fm.id " +
                        ") " +
                        "SELECT " +
                        "   fm.id, " +
                        "   fm.title, " +
                        "   fm.year, " +
                        "   fm.director, " +
                        "   fm.rating, " +
                        "   mg.genres, " +
                        "   ts.star_ids, " +
                        "   ts.star_names " +
                        "FROM FilteredMovies fm " +
                        "LEFT JOIN MovieGenres mg ON fm.id = mg.movie_id " +
                        "LEFT JOIN TopStars ts ON fm.id = ts.movie_id " +
                        "ORDER BY ";

        if ("rating".equalsIgnoreCase(sort1)) {
            movieQuery += "fm.rating IS NULL, ";
        }

        movieQuery += col1 + " " + dir1 + ", ";

        if ("rating".equalsIgnoreCase(sort2)) {
            movieQuery += "fm.rating IS NULL, ";
        }

        movieQuery += col2 + " " + dir2 + " ";
        movieQuery += "LIMIT ? OFFSET ?";

        try (Connection dbCon = dataSource.getConnection();
             PreparedStatement movieStatement = dbCon.prepareStatement(movieQuery)) {

            int paramIndex = 1;
            movieStatement.setString(paramIndex++, fullTextQuery);
            movieStatement.setInt(paramIndex++, pageSize + 1);
            movieStatement.setInt(paramIndex++, offset);

            ResultSet rs = movieStatement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("movie_id", rs.getString("id"));
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getInt("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));

                String genres = rs.getString("genres");
                jsonObject.addProperty("movie_genres", genres == null ? "" : genres);

                float rating = rs.getFloat("rating");
                if (rs.wasNull()) {
                    jsonObject.add("movie_rating", null);
                } else {
                    jsonObject.addProperty("movie_rating", rating);
                }

                JsonArray starArray = new JsonArray();
                String starIds = rs.getString("star_ids");
                String starNames = rs.getString("star_names");

                if (starIds != null && starNames != null) {
                    String[] starIdArray = starIds.split(",");
                    String[] starNameArray = starNames.split("\t");

                    for (int i = 0; i < starIdArray.length && i < starNameArray.length; i++) {
                        JsonObject singleStar = new JsonObject();
                        singleStar.addProperty("star_id", starIdArray[i]);
                        singleStar.addProperty("star_name", starNameArray[i]);
                        starArray.add(singleStar);
                    }
                }

                if (starArray.size() == 0) {
                    JsonObject noStar = new JsonObject();
                    noStar.addProperty("star_id", "");
                    noStar.addProperty("star_name", "N/A");
                    starArray.add(noStar);
                }

                jsonObject.add("movie_stars", starArray);
                jsonArray.add(jsonObject);
            }

            String finalResult = jsonArray.toString();

            if (cacheKey != null) {
                queryCache.put(cacheKey, finalResult);
            }

            out.write(finalResult);

            HttpSession session = request.getSession(true);
            session.setAttribute("lastMovieQuery", "movieList.html?" + cacheKey);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private String buildFullTextQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        String[] tokens = query.trim().split("\\s+");
        List<String> parts = new ArrayList<>();

        for (String token : tokens) {
            token = token.replaceAll("[^a-zA-Z0-9]", "");

            if (!token.isEmpty()) {
                parts.add("+" + token + "*");
            }
        }

        return String.join(" ", parts);
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}