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

@WebServlet("/single-movie")
public class SingleMovieServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("text/html");
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
                        "JOIN stars_in_movies sim ON s.id = sim.star_id " +
                        "WHERE sim.movie_id = ? " +
                        "ORDER BY s.name";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                out.println("<html>");
                out.println("<head><title>Single Movie Page</title></head>");
                out.println("<body>");

                out.println("<h1>Movie Details</h1>");
                out.println("<p><a href='movie-list'>Back to Movie List</a></p>");

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

                            out.println("<p><strong>Title:</strong> " + title + "</p>");
                            out.println("<p><strong>Year:</strong> " + year + "</p>");
                            out.println("<p><strong>Director:</strong> " + director + "</p>");
                            out.println("<p><strong>Genres:</strong> " + genres + "</p>");
                            out.println("<p><strong>Rating:</strong> " + rating + "</p>");

                            out.println("<p><strong>Stars:</strong> ");

                            StringBuilder starLinks = new StringBuilder();

                            try (PreparedStatement starStatement = conn.prepareStatement(starQuery)) {
                                starStatement.setString(1, movieId);

                                try (ResultSet starRs = starStatement.executeQuery()) {
                                    while (starRs.next()) {
                                        String starId = starRs.getString("id");
                                        String starName = starRs.getString("name");

                                        if (starLinks.length() > 0) {
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

                            out.println(starLinks + "</p>");
                        }
                    }
                }

                if (!movieFound) {
                    out.println("<p>Movie not found.</p>");
                }

                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleMovieServlet", e);
            out.println("<html><body>");
            out.println("<h2>Error</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</body></html>");
        } finally {
            out.close();
        }
    }
}