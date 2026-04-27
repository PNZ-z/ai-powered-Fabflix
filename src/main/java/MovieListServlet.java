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
import java.util.concurrent.ConcurrentHashMap;
@WebServlet("/api/movie-list")
public class MovieListServlet extends HttpServlet {
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
        return (value != null) ? value.trim() : null;
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String cacheKey = request.getQueryString();

        // 3. Check if we have this result in the cache
        if (cacheKey != null && queryCache.containsKey(cacheKey)) {
            System.out.println("Cache Hit! Returning cached results for: " + cacheKey);
            out.write(queryCache.get(cacheKey));
            out.close();
            return; // Stop here, no need to talk to MySQL!
        }

        System.out.println("Cache Miss. Querying database...");
        String searchTitle = getTrimmedParameter(request, "title");
        String searchYear = getTrimmedParameter(request, "year");
        String searchDirector = getTrimmedParameter(request, "director");
        String searchStar = getTrimmedParameter(request, "star");
        String searchPageNumber = getTrimmedParameter(request, "pageNumber");
        String pageSizeParam = getTrimmedParameter(request, "pageSize");
        int pageSize = 10;

        if (pageSizeParam != null && !pageSizeParam.trim().isEmpty()) {
            pageSize = Integer.parseInt(pageSizeParam);
        }

        String sort1 = request.getParameter("sort1");
        String dir1 = request.getParameter("dir1");
        String sort2 = request.getParameter("sort2");
        String dir2 = request.getParameter("dir2");
        if (sort1 == null) sort1 = "title";
        if (dir1 == null) dir1 = "ASC";
        if (sort2 == null) sort2 = "rating";
        if (dir2 == null) dir2 = "DESC";
        String searchGenre = request.getParameter("genre");
        String startsWith = request.getParameter("startsWith");
        

        String movieQuery =
                "SELECT " +
                        "m.id, " +
                        "m.title, " +
                        "m.year, " +
                        "m.director, " +
                        "r.rating, " +
                        "SUBSTRING_INDEX(" +
                        "   GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', '), " +
                        "   ', ', " +
                        "   3" +
                        ") AS genres " +
                        "FROM movies m " +
                        "JOIN ratings r ON m.id = r.movie_id " +
                        "LEFT JOIN genres_in_movies gim ON m.id = gim.movie_id " +
                        "LEFT JOIN genres g ON gim.genre_id = g.id ";

        String starQuery =
                "SELECT s.id, s.name " +
                        "FROM stars s " +
                        "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                        "JOIN ( " +
                        "   SELECT star_id, COUNT(movie_id) AS movie_count " +
                        "   FROM stars_in_movies " +
                        "   GROUP BY star_id " +
                        ") AS star_popularity ON s.id = star_popularity.star_id " +
                        "WHERE sim.movie_id = ? " +
                        "ORDER BY star_popularity.movie_count DESC, s.name ASC " +
                        "LIMIT 3";

        try {


            if (searchStar != null && !searchStar.trim().isEmpty()) {
                movieQuery += " JOIN stars_in_movies sim ON m.id = sim.movie_id " +
                        " JOIN stars s ON s.id = sim.star_id ";
            }

            if (searchGenre != null && !searchGenre.trim().isEmpty()) {
                movieQuery += " JOIN genres_in_movies gim_filter ON m.id = gim_filter.movie_id " +
                        " JOIN genres g_filter ON gim_filter.genre_id = g_filter.id ";
            }

            movieQuery += " WHERE 1=1 ";

            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                movieQuery += " AND m.title LIKE ? ";
            } else {
                System.out.println("title not detected");
            }

            if (searchYear != null && !searchYear.trim().isEmpty()) {
                movieQuery += " AND m.year = ? ";
            }

            if (searchDirector != null && !searchDirector.trim().isEmpty()) {
                movieQuery += " AND m.director LIKE ? ";
            }

            if (searchStar != null && !searchStar.trim().isEmpty()) {
                movieQuery += " AND s.name LIKE ? ";
            }

            if (searchGenre != null && !searchGenre.trim().isEmpty()) {
                movieQuery += " AND g_filter.name = ? ";
            }

            if (startsWith != null && !startsWith.trim().isEmpty()) {
                movieQuery += " AND UPPER(m.title) LIKE CONCAT(UPPER(?), '%') ";
            }

            movieQuery += " GROUP BY m.id, m.title, m.year, m.director, r.rating ";
            String col1 = sort1.equals("rating") ? "r.rating" : "m.title";
            String col2 = sort2.equals("rating") ? "r.rating" : "m.title";
            movieQuery += " ORDER BY " + col1 + " " + dir1 + ", " + col2 + " " + dir2;

            movieQuery +=" LIMIT ? ";

            if (searchPageNumber != null && !searchPageNumber.trim().isEmpty() && isInteger(searchPageNumber)) {
                movieQuery += " OFFSET " + Integer.parseInt(searchPageNumber) * pageSize;
            } else {
                movieQuery += " OFFSET 0";
            }

            try (Connection dbCon = dataSource.getConnection();
                 PreparedStatement movieStatement = dbCon.prepareStatement(movieQuery))
            {

                int paramIndex = 1;

                if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + searchTitle + "%");
                }

                if (searchYear != null && !searchYear.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, searchYear);
                }

                if (searchDirector != null && !searchDirector.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + searchDirector + "%");
                }

                if (searchStar != null && !searchStar.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, "%" + searchStar + "%");
                }

                if (searchGenre != null && !searchGenre.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, searchGenre);
                }

                if (startsWith != null && !startsWith.trim().isEmpty()) {
                    movieStatement.setString(paramIndex++, startsWith);
                }

                    movieStatement.setInt(paramIndex++, pageSize + 1);


                System.out.println(movieStatement.toString());

                ResultSet rs = movieStatement.executeQuery();
                JsonArray jsonArray = new JsonArray();

                while (rs.next()) {
                    String movieId = rs.getString("id");
                    String title = rs.getString("title");
                    int year = rs.getInt("year");
                    String director = rs.getString("director");
                    String genres = rs.getString("genres");
                    float rating = rs.getFloat("rating");

                    if (genres == null) {
                        genres = "";
                    }

                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("movie_id", movieId);
                    jsonObject.addProperty("movie_title", title);
                    jsonObject.addProperty("movie_year", year);
                    jsonObject.addProperty("movie_director", director);
                    jsonObject.addProperty("movie_genres", genres);
                    jsonObject.addProperty("movie_rating", rating);

                    JsonArray starArray = new JsonArray();

                    try (PreparedStatement starStatement = dbCon.prepareStatement(starQuery)) {
                        starStatement.setString(1, movieId);

                        try (ResultSet starRs = starStatement.executeQuery()) {
                            while (starRs.next()) {
                                String starId = starRs.getString("id");
                                String starName = starRs.getString("name");

                                JsonObject singleStar = new JsonObject();
                                singleStar.addProperty("star_id", starId);
                                singleStar.addProperty("star_name", starName);
                                starArray.add(singleStar);
                            }
                            jsonObject.add("movie_stars", starArray);
                        }
                    }

                    jsonArray.add(jsonObject);

                }
                String finalResult = jsonArray.toString();

                // 4. Save the result to the cache before sending it back
                if (cacheKey != null) {
                    queryCache.put(cacheKey, finalResult);
                }

                out.write(finalResult);

                // saving query for "Back to List" functionality
                HttpSession session = request.getSession(true);
                session.setAttribute("lastMovieQuery", cacheKey);

            }

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
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