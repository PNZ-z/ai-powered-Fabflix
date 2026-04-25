import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {

    private final String loginUser = "mytestuser";
    private final String loginPasswd = "My6$Password";
    private final String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

    private double getMoviePrice(String movieId) {
        int hash = Math.abs(movieId.hashCode());
        return 5.00 + (hash % 1500) / 100.0;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();

        @SuppressWarnings("unchecked")
        Map<String, Integer> cart =
                (Map<String, Integer>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        JsonArray cartJsonArray = new JsonArray();

        String query = "SELECT id, title FROM movies WHERE id = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            PreparedStatement statement = conn.prepareStatement(query);

            for (String movieId : cart.keySet()) {
                statement.setString(1, movieId);

                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    int quantity = cart.get(movieId);
                    double price = getMoviePrice(movieId);

                    JsonObject movieJsonObject = new JsonObject();
                    movieJsonObject.addProperty("movie_id", rs.getString("id"));
                    movieJsonObject.addProperty("movie_title", rs.getString("title"));
                    movieJsonObject.addProperty("quantity", quantity);
                    movieJsonObject.addProperty("price", price);

                    cartJsonArray.add(movieJsonObject);
                }

                rs.close();
            }

            statement.close();
            conn.close();

            response.setContentType("application/json");
            response.getWriter().write(cartJsonArray.toString());

        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write(e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");

        if (movieId == null || action == null) {
            response.setStatus(400);
            response.getWriter().write("Missing movieId or action");
            return;
        }

        HttpSession session = request.getSession();

        @SuppressWarnings("unchecked")
        Map<String, Integer> cart =
                (Map<String, Integer>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        synchronized (cart) {
            int currentQuantity = cart.getOrDefault(movieId, 0);

            if ("add".equals(action) || "increase".equals(action)) {
                cart.put(movieId, currentQuantity + 1);
            } else if ("decrease".equals(action)) {
                if (currentQuantity <= 1) {
                    cart.remove(movieId);
                } else {
                    cart.put(movieId, currentQuantity - 1);
                }
            } else if ("delete".equals(action)) {
                cart.remove(movieId);
            } else {
                response.setStatus(400);
                response.getWriter().write("Unknown action");
                return;
            }
        }

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "success");

        response.setContentType("application/json");
        response.getWriter().write(responseJsonObject.toString());
    }
}