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
        String loginUser = "root";
        String loginPasswd = "20041113";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("text/html");
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
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Movie List</title>");
                out.println("</head>");
                out.println("<body>");

                out.println("<h1>Top 20 Rated Movies</h1>");
                out.println("<table border='1'>");
                out.println("<tr>");
                out.println("<th>Title</th>");
                out.println("<th>Year</th>");
                out.println("<th>Director</th>");
                out.println("<th>Genres</th>");
                out.println("<th>Stars</th>");
                out.println("<th>Rating</th>");
                out.println("</tr>");

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

                    try (PreparedStatement starStatement = conn.prepareStatement(starQuery)) {
                        starStatement.setString(1, movieId);

                        try (ResultSet starRs = starStatement.executeQuery()) {
                            while (starRs.next()) {
                                String starId = starRs.getString("id");
                                String starName = starRs.getString("name");

                                if (!starLinks.isEmpty()) {
                                    starLinks.append(", ");
                                }

                                starLinks.append("<a href='single-star?id=")
                                        .append(starId)
                                        .append("'>")
                                        .append(starName)
                                        .append("</a>");
                            }
                        }
                    }

                    out.println("<tr>");
                    out.println("<td><a href='single-movie?id=" + movieId + "'>" + title + "</a></td>");
                    out.println("<td>" + year + "</td>");
                    out.println("<td>" + director + "</td>");
                    out.println("<td>" + genres + "</td>");
                    out.println("<td>" + starLinks + "</td>");
                    out.println("<td>" + rating + "</td>");
                    out.println("</tr>");
                }

                out.println("</table>");
                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            request.getServletContext().log("Error in MovieListServlet", e);
            out.println("<html><body>");
            out.println("<h2>Error</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</body></html>");
        } finally {
            out.close();
        }
    }
}