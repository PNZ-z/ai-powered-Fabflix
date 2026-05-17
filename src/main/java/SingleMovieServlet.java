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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@WebServlet("/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";

    private MongoClient mongoClient;
    private MongoCollection<Document> moviesCollection;

    @Override
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        moviesCollection = database.getCollection("movies");
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
        String movieId = request.getParameter("id");

        try {
            Document movie = moviesCollection.find(Filters.eq("_id", movieId)).first();

            if (movie == null) {
                JsonObject errorObject = new JsonObject();
                errorObject.addProperty("errorMessage", "Movie not found.");
                response.setStatus(404);
                out.write(errorObject.toString());
                return;
            }

            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("movie_id", movie.getString("_id"));
            jsonObject.addProperty("movie_title", movie.getString("title"));
            jsonObject.addProperty("movie_year", movie.getInteger("year"));
            jsonObject.addProperty("movie_director", movie.getString("director"));
            jsonObject.addProperty("movie_genres", getGenreText(movie));
            jsonObject.addProperty("movie_rating", getRatingText(movie));
            jsonObject.add("movie_stars", getStarsJsonArray(movie));

            response.setStatus(200);
            out.write(jsonObject.toString());
        } catch (Exception e) {
            request.getServletContext().log("Error in SingleMovieServlet", e);

            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());

            response.setStatus(500);
            out.write(errorObject.toString());
        } finally {
            out.close();
        }
    }

    @SuppressWarnings("unchecked")
    private String getGenreText(Document movie) {
        List<Document> genres = (List<Document>) movie.get("genres");

        if (genres == null || genres.isEmpty()) {
            return "";
        }

        List<String> genreNames = new ArrayList<>();

        for (Document genre : genres) {
            String name = genre.getString("name");

            if (name != null && !name.trim().isEmpty()) {
                genreNames.add(name.trim());
            }
        }

        genreNames.sort(String::compareToIgnoreCase);

        return String.join(", ", genreNames);
    }

    private String getRatingText(Document movie) {
        Document ratingDocument = movie.get("rating", Document.class);

        if (ratingDocument == null) {
            return "N/A";
        }

        Object ratingValue = ratingDocument.get("rating");

        if (ratingValue == null) {
            return "N/A";
        }

        return String.valueOf(((Number) ratingValue).floatValue());
    }

    @SuppressWarnings("unchecked")
    private JsonArray getStarsJsonArray(Document movie) {
        JsonArray starArray = new JsonArray();

        List<Document> stars = (List<Document>) movie.get("stars");

        if (stars == null || stars.isEmpty()) {
            JsonObject noStar = new JsonObject();
            noStar.addProperty("star_id", "");
            noStar.addProperty("star_name", "N/A");
            starArray.add(noStar);
            return starArray;
        }

        stars.sort(Comparator.comparing(
                star -> {
                    String name = star.getString("name");
                    return name == null ? "" : name.toLowerCase();
                }
        ));

        for (Document star : stars) {
            JsonObject singleStar = new JsonObject();
            singleStar.addProperty("star_id", star.getString("star_id"));
            singleStar.addProperty("star_name", star.getString("name"));
            starArray.add(singleStar);
        }

        return starArray;
    }
}