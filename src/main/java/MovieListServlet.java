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

@WebServlet("/api/movie-list")
public class MovieListServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String searchTitle = request.getParameter("title");
        String searchYear = request.getParameter("year");
        String searchDirector = request.getParameter("director");
        String searchStar = request.getParameter("star");
        String searchPageNumber = request.getParameter("pageNumber");

        System.out.println("DEBUG: searchTitle value is [" + searchTitle + "]");



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

//

        String starQuery =
                "SELECT s.id, s.name " +
                        "FROM stars s " +
                        "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                        "WHERE sim.movie_id = ? " +
                        "ORDER BY s.name " +
                        "LIMIT 3";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (searchStar != null && !searchStar.trim().isEmpty()) {
                movieQuery+=" JOIN stars_in_movies sim ON m.id = sim.movie_id JOIN stars s ON s.id = sim.star_id ";
            }
            movieQuery += " WHERE 1=1 ";
            if(searchTitle != null && !searchTitle.trim().isEmpty())
                movieQuery += " AND m.title LIKE ? ";
            else
                System.out.println("title not detected");
            if(searchYear != null && !searchYear.trim().isEmpty())
                movieQuery += " AND m.year= ? ";
            if(searchDirector != null && !searchDirector.trim().isEmpty())
                movieQuery += " AND m.director LIKE ? ";
            if(searchStar != null && !searchStar.trim().isEmpty())
                movieQuery += " AND s.name LIKE ? ";

            movieQuery += " GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                            "ORDER BY r.rating DESC " +
                            "LIMIT 10 ";

            if(searchPageNumber != null && !searchPageNumber.trim().isEmpty()&&isInteger(searchPageNumber))
                movieQuery += " OFFSET " + Integer.parseInt(searchPageNumber)*10;
            else{
                movieQuery += " OFFSET 0";
            }

            try (
                    Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            ) {
                PreparedStatement movieStatement = conn.prepareStatement(movieQuery);
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



                    // Create a JsonObject based on the data we retrieve from rs
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("movie_id", movieId);
                    jsonObject.addProperty("movie_title", title);
                    jsonObject.addProperty("movie_year", year);
                    jsonObject.addProperty("movie_director", director);
                    jsonObject.addProperty("movie_genres", genres);
                    jsonObject.addProperty("movie_rating", rating);
                    //creating a new osobj for storing info for stars and the add this into main jsobj
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
                            jsonObject.add("movie_stars",starArray);
                        }
                    }




                    jsonArray.add(jsonObject);

                }

                out.write(jsonArray.toString());
            }
        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
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