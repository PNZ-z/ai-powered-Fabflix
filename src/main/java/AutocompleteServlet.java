import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {

    private MongoClient mongoClient;
    private MongoDatabase database;

    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=moviedb_mongo";

    private static final String MONGO_DB_NAME = "moviedb_mongo";

    @Override
    public void init() throws ServletException {
        mongoClient = MongoClients.create(MONGO_URI);
        database = mongoClient.getDatabase(MONGO_DB_NAME);
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonArray jsonArray = new JsonArray();

        String query = request.getParameter("query");

        if (query == null || query.trim().isEmpty()) {
            response.getWriter().write(jsonArray.toString());
            return;
        }

        query = query.trim();

        if (query.length() < 3) {
            response.getWriter().write(jsonArray.toString());
            return;
        }

        try {
            MongoCollection<Document> moviesCollection =
                    database.getCollection("movies");

            Bson filter = buildPrefixSearchFilter(query);

            for (Document movie : moviesCollection.find(filter).limit(10)) {
                String movieId = movie.getString("_id");
                String movieTitle = movie.getString("title");

                jsonArray.add(generateJsonObject(movieId, movieTitle));
            }

            response.getWriter().write(jsonArray.toString());

        } catch (Exception e) {
            request.getServletContext().log("AutocompleteServlet error", e);
            response.setStatus(500);
            response.getWriter().write(jsonArray.toString());
        }
    }

    private static Bson buildPrefixSearchFilter(String query) {
        String[] tokens = query.trim().split("\\s+");

        List<Bson> filters = new ArrayList<>();

        for (String token : tokens) {
            token = token.replaceAll("[^a-zA-Z0-9]", "");

            if (!token.isEmpty()) {
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(token),
                        Pattern.CASE_INSENSITIVE);

                filters.add(Filters.regex("title", pattern));
            }
        }

        if (filters.isEmpty()) {
            return new Document();
        }

        return Filters.and(filters);
    }

    private static JsonObject generateJsonObject(String movieId, String movieTitle) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("value", movieTitle);

        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("movie_id", movieId);

        jsonObject.add("data", dataObject);

        return jsonObject;
    }
}
