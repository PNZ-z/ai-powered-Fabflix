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

@WebServlet("/single-star")
public class SingleStarServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "root";
        String loginPasswd = "20041113";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        response.setContentType("text/html");
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
                        "ORDER BY m.title";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
                out.println("<html>");
                out.println("<head><title>Single Star Page</title></head>");
                out.println("<body>");

                out.println("<h1>Star Details</h1>");
                out.println("<p><a href='movie-list'>Back to Movie List</a></p>");

                boolean starFound = false;

                try (PreparedStatement starStatement = conn.prepareStatement(starQuery)) {
                    starStatement.setString(1, starId);

                    try (ResultSet rs = starStatement.executeQuery()) {
                        if (rs.next()) {
                            starFound = true;

                            String name = rs.getString("name");
                            int birthYear = rs.getInt("birth_year");
                            boolean birthYearIsNull = rs.wasNull();

                            out.println("<p><strong>Name:</strong> " + name + "</p>");

                            if (birthYearIsNull) {
                                out.println("<p><strong>Birth Year:</strong> N/A</p>");
                            } else {
                                out.println("<p><strong>Birth Year:</strong> " + birthYear + "</p>");
                            }

                            out.println("<p><strong>Movies:</strong> ");

                            StringBuilder movieLinks = new StringBuilder();

                            try (PreparedStatement movieStatement = conn.prepareStatement(movieQuery)) {
                                movieStatement.setString(1, starId);

                                try (ResultSet movieRs = movieStatement.executeQuery()) {
                                    while (movieRs.next()) {
                                        String movieId = movieRs.getString("id");
                                        String movieTitle = movieRs.getString("title");

                                        if (movieLinks.length() > 0) {
                                            movieLinks.append(", ");
                                        }

                                        movieLinks.append("<a href='single-movie?id=")
                                                .append(movieId)
                                                .append("'>")
                                                .append(movieTitle)
                                                .append("</a>");
                                    }
                                }
                            }

                            out.println(movieLinks + "</p>");
                        }
                    }
                }

                if (!starFound) {
                    out.println("<p>Star not found.</p>");
                }

                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleStarServlet", e);
            out.println("<html><body>");
            out.println("<h2>Error</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</body></html>");
        } finally {
            out.close();
        }
    }
}