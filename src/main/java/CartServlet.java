import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.bson.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";
    private static final String MOVIES_COLLECTION = "movies";

    private MongoClient mongoClient;
    private MongoCollection<Document> moviesCollection;

    @Override
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        moviesCollection = database.getCollection(MOVIES_COLLECTION);
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    private double getMoviePrice(String movieId) {
        int hash = Math.abs(movieId.hashCode());
        return 5.00 + (hash % 1500) / 100.0;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        HttpSession session = request.getSession();

        @SuppressWarnings("unchecked")
        Map<String, Integer> cart =
                (Map<String, Integer>) session.getAttribute("cart");

        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        JsonArray cartJsonArray = new JsonArray();

        if (cart.isEmpty()) {
            response.getWriter().write(cartJsonArray.toString());
            return;
        }

        try {
            Set<String> movieIds = new HashSet<>(cart.keySet());

            FindIterable<Document> movies = moviesCollection
                    .find(Filters.in("_id", movieIds))
                    .projection(new Document("_id", 1).append("title", 1));

            for (Document movie : movies) {
                String movieId = movie.getString("_id");
                String movieTitle = movie.getString("title");

                int quantity = cart.getOrDefault(movieId, 0);
                double price = getMoviePrice(movieId);

                JsonObject movieJsonObject = new JsonObject();
                movieJsonObject.addProperty("movie_id", movieId);
                movieJsonObject.addProperty("movie_title", movieTitle);
                movieJsonObject.addProperty("quantity", quantity);
                movieJsonObject.addProperty("price", price);

                cartJsonArray.add(movieJsonObject);
            }

            response.getWriter().write(cartJsonArray.toString());
        } catch (Exception e) {
            response.setStatus(500);

            JsonObject errorJsonObject = new JsonObject();
            errorJsonObject.addProperty("errorMessage", e.getMessage());

            response.getWriter().write(errorJsonObject.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");

        if (movieId == null || action == null) {
            response.setStatus(400);

            JsonObject errorJsonObject = new JsonObject();
            errorJsonObject.addProperty("errorMessage", "Missing movieId or action");

            response.getWriter().write(errorJsonObject.toString());
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

                JsonObject errorJsonObject = new JsonObject();
                errorJsonObject.addProperty("errorMessage", "Unknown action");

                response.getWriter().write(errorJsonObject.toString());
                return;
            }
        }

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "success");

        response.getWriter().write(responseJsonObject.toString());
    }
}