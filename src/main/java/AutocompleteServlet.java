import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Unable to load moviedb datasource", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonArray jsonArray = new JsonArray();

        String query = request.getParameter("query");
        if (query == null || query.trim().length() < 3) {
            response.getWriter().write(jsonArray.toString());
            return;
        }

        String fullTextQuery = buildFullTextQuery(query);
        if (fullTextQuery.isEmpty()) {
            response.getWriter().write(jsonArray.toString());
            return;
        }

        String sql =
                "SELECT id, title " +
                        "FROM movies " +
                        "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) " +
                        "LIMIT 10";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullTextQuery);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("value", rs.getString("title"));

                    JsonObject dataObject = new JsonObject();
                    dataObject.addProperty("movie_id", rs.getString("id"));

                    jsonObject.add("data", dataObject);
                    jsonArray.add(jsonObject);
                }
            }

            response.getWriter().write(jsonArray.toString());
        } catch (Exception e) {
            request.getServletContext().log("AutocompleteServlet error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(jsonArray.toString());
        }
    }

    private String buildFullTextQuery(String query) {
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
}