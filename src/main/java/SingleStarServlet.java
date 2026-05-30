import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/single-star")
public class SingleStarServlet extends HttpServlet {

    // 1. Declare the DataSource
    private DataSource dataSource;

    // 2. Add the init() method to initialize the connection pool
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String starId = request.getParameter("id");

        String starQuery =
                "SELECT s.id, s.name, s.birth_year " +
                        "FROM stars s " +
                        "WHERE s.id = ?";

        String movieQuery =
                "SELECT m.id, m.title " +
                        "FROM movies m " +
                        "JOIN stars_in_movies sim ON m.id = sim.movie_id " +
                        "WHERE sim.star_id = ? " +
                        "ORDER BY m.year DESC, m.title ASC";

        try {
            // 3. Connect using dataSource instead of DriverManager
            try (Connection conn = dataSource.getConnection()) {
                JsonObject jsonObject = new JsonObject();
                boolean starFound = false;

                try (PreparedStatement starStatement = conn.prepareStatement(starQuery)) {
                    starStatement.setString(1, starId);

                    try (ResultSet rs = starStatement.executeQuery()) {
                        if (rs.next()) {
                            starFound = true;

                            String name = rs.getString("name");
                            int birthYear = rs.getInt("birth_year");
                            boolean birthYearIsNull = rs.wasNull();

                            jsonObject.addProperty("star_id", starId);
                            jsonObject.addProperty("star_name", name);

                            if (birthYearIsNull) {
                                jsonObject.addProperty("star_birth_year", "N/A");
                            } else {
                                jsonObject.addProperty("star_birth_year", birthYear);
                            }

                            JsonArray movieArray = new JsonArray();

                            try (PreparedStatement movieStatement = conn.prepareStatement(movieQuery)) {
                                movieStatement.setString(1, starId);

                                try (ResultSet movieRs = movieStatement.executeQuery()) {
                                    while (movieRs.next()) {
                                        String movieId = movieRs.getString("id");
                                        String movieTitle = movieRs.getString("title");

                                        JsonObject singleMovie = new JsonObject();
                                        singleMovie.addProperty("movie_id", movieId);
                                        singleMovie.addProperty("movie_title", movieTitle);
                                        movieArray.add(singleMovie);
                                    }
                                }
                            }

                            jsonObject.add("star_movies", movieArray);
                        }
                    }
                }

                if (!starFound) {
                    JsonObject errorObject = new JsonObject();
                    errorObject.addProperty("errorMessage", "Star not found.");
                    out.write(errorObject.toString());
                    response.setStatus(404);
                } else {
                    out.write(jsonObject.toString());
                    response.setStatus(200);
                }
            }
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleStarServlet", e);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}