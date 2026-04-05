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

@WebServlet("/movie-list")
public class MovieListServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

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
                        "LEFT JOIN genres g ON gim.genre_id = g.id " +
                        "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                        "ORDER BY r.rating DESC " +
                        "LIMIT 20";

        String starQuery =
                "SELECT s.id, s.name " +
                        "FROM stars s " +
                        "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                        "WHERE sim.movie_id = ? " +
                        "ORDER BY s.name " +
                        "LIMIT 3";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                    Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                    PreparedStatement movieStatement = conn.prepareStatement(movieQuery);
                    ResultSet rs = movieStatement.executeQuery()
            ) {
//                out.println("<html>");
//                out.println("<head>");
//                out.println("<title>Movie List</title>");
//                out.println("</head>");
//                out.println("<body>");
//
//                out.println("<h1>Top 20 Rated Movies</h1>");
//                out.println("<table border='1'>");
//                out.println("<tr>");
//                out.println("<th>Title</th>");
//                out.println("<th>Year</th>");
//                out.println("<th>Director</th>");
//                out.println("<th>Genres</th>");
//                out.println("<th>Stars</th>");
//                out.println("<th>Rating</th>");
//                out.println("</tr>");

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


                    StringBuilder starLinks = new StringBuilder();

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

//                                if (starLinks.length()>0) {
//                                    starLinks.append(", ");
//                                }
//
//                                starLinks.append("<a href='single-star?id=")
//                                        .append(starId)
//                                        .append("'>")
//                                        .append(starName)
//                                        .append("</a>");
                            }
                            jsonObject.add("movie_stars",starArray);
                        }
                    }




                    jsonArray.add(jsonObject);
//                    out.println("<tr>");
//                    out.println("<td><a href='single-movie?id=" + movieId + "'>" + title + "</a></td>");
//                    out.println("<td>" + year + "</td>");
//                    out.println("<td>" + director + "</td>");
//                    out.println("<td>" + genres + "</td>");
//                    out.println("<td>" + starLinks + "</td>");
//                    out.println("<td>" + rating + "</td>");
//                    out.println("</tr>");
                }

//                out.println("</table>");
//                out.println("</body>");
//                out.println("</html>");
                out.write(jsonArray.toString());
            }
        } catch (Exception e) {
//            request.getServletContext().log("Error in MovieListServlet", e);
//            out.println("<html><body>");
//            out.println("<h2>Error</h2>");
//            out.println("<p>" + e.getMessage() + "</p>");
//            out.println("</body></html>");



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
}