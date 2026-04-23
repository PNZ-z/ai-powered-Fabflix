import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String movieId = request.getParameter("id");

        String movieQuery =
                "SELECT " +
                        "m.id, " +
                        "m.title, " +
                        "m.year, " +
                        "m.director, " +
                        "r.rating, " +
                        "GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR ', ') AS genres " +
                        "FROM movies m " +
                        "JOIN ratings r ON m.id = r.movie_id " +
                        "LEFT JOIN genres_in_movies gim ON m.id = gim.movie_id " +
                        "LEFT JOIN genres g ON gim.genre_id = g.id " +
                        "WHERE m.id = ? " +
                        "GROUP BY m.id, m.title, m.year, m.director, r.rating";

        String starQuery =
                "SELECT s.id, s.name " +
                        "FROM stars s " +
                        "JOIN stars_in_movies sim1 ON s.id = sim1.star_id " +
                        "JOIN stars_in_movies sim2 ON s.id = sim2.star_id " +
                        "WHERE sim1.movie_id = ? " +
                        "GROUP BY s.id, s.name " +
                        "ORDER BY COUNT(sim2.movie_id) DESC, s.name ASC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                JsonObject jsonObject = new JsonObject();
                boolean movieFound = false;

                try (PreparedStatement movieStatement = conn.prepareStatement(movieQuery)) {
                    movieStatement.setString(1, movieId);

                    try (ResultSet rs = movieStatement.executeQuery()) {
                        if (rs.next()) {
                            movieFound = true;

                            String title = rs.getString("title");
                            int year = rs.getInt("year");
                            String director = rs.getString("director");
                            String genres = rs.getString("genres");
                            float rating = rs.getFloat("rating");

                            if (genres == null) {
                                genres = "";
                            }

                            jsonObject.addProperty("movie_id", movieId);
                            jsonObject.addProperty("movie_title", title);
                            jsonObject.addProperty("movie_year", year);
                            jsonObject.addProperty("movie_director", director);
                            jsonObject.addProperty("movie_genres", genres);
                            jsonObject.addProperty("movie_rating", rating);

                            JsonArray starArray = new JsonArray();

                            try (PreparedStatement starStatement = conn.prepareStatement(starQuery)) {
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
                                }
                            }

                            jsonObject.add("movie_stars", starArray);
                        }
                    }
                }

                if (!movieFound) {
                    JsonObject errorObject = new JsonObject();
                    errorObject.addProperty("errorMessage", "Movie not found.");
                    out.write(errorObject.toString());
                    response.setStatus(404);
                } else {
                    out.write(jsonObject.toString());
                    response.setStatus(200);
                }
            }
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleMovieServlet", e);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}