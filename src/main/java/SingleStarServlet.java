import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;

@WebServlet("/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";

    private MongoClient mongoClient;
    private MongoCollection<Document> starsCollection;

    @Override
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        starsCollection = database.getCollection("stars");
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String starId = request.getParameter("id");

        try {
            Document star = starsCollection.find(Filters.eq("_id", starId)).first();

            if (star == null) {
                JsonObject errorObject = new JsonObject();
                errorObject.addProperty("errorMessage", "Star not found.");
                response.setStatus(404);
                out.write(errorObject.toString());
                return;
            }

            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("star_id", star.getString("_id"));
            jsonObject.addProperty("star_name", star.getString("name"));

            Integer birthYear = star.getInteger("birth_year");
            if (birthYear == null) {
                jsonObject.addProperty("star_birth_year", "N/A");
            } else {
                jsonObject.addProperty("star_birth_year", birthYear);
            }

            jsonObject.add("star_movies", getMoviesJsonArray(star));

            response.setStatus(200);
            out.write(jsonObject.toString());
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleStarServlet", e);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());

            response.setStatus(500);
            out.write(errorObject.toString());
        } finally {
            out.close();
        }
    }

    @SuppressWarnings("unchecked")
    private JsonArray getMoviesJsonArray(Document star) {
        JsonArray movieArray = new JsonArray();

        List<Document> movies = (List<Document>) star.get("movies");

        if (movies == null || movies.isEmpty()) {
            return movieArray;
        }

        movies.sort(Comparator
                .comparing((Document movie) -> {
                    Integer year = movie.getInteger("year");
                    return year == null ? 0 : year;
                }).reversed()
                .thenComparing(movie -> {
                    String title = movie.getString("title");
                    return title == null ? "" : title.toLowerCase();
                }));

        for (Document movie : movies) {
            JsonObject singleMovie = new JsonObject();
            singleMovie.addProperty("movie_id", movie.getString("movie_id"));
            singleMovie.addProperty("movie_title", movie.getString("title"));
            movieArray.add(singleMovie);
        }

        return movieArray;
    }
}