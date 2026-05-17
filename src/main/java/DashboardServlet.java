import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard")
public class DashboardServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> moviesCollection;
    private MongoCollection<Document> starsCollection;
    private MongoCollection<Document> genresCollection;

    @Override
    public void init() throws ServletException {
        mongoClient = MongoClients.create(MONGO_URI);
        database = mongoClient.getDatabase(MONGO_DB_NAME);
        moviesCollection = database.getCollection("movies");
        starsCollection = database.getCollection("stars");
        genresCollection = database.getCollection("genres");
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        if ("metadata".equals(action)) {
            handleMetadata(response);
        } else {
            writeError(response, "Invalid GET action.");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        if ("add-star".equals(action)) {
            handleAddStar(request, response);
        } else if ("add-movie".equals(action)) {
            handleAddMovie(request, response);
        } else {
            writeError(response, "Invalid POST action.");
        }
    }

    private void handleAddStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();

        String starName = clean(request.getParameter("star_name"));
        String birthYearString = clean(request.getParameter("birth_year"));

        if (starName.isEmpty()) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Star name cannot be empty.");
            writeJson(response, responseJsonObject);
            return;
        }

        Integer birthYear = parseOptionalYear(birthYearString, "Birth year", responseJsonObject);
        if (responseJsonObject.has("status")) {
            writeJson(response, responseJsonObject);
            return;
        }

        try {
            String newStarId = generateNewPrefixedId(starsCollection, "DS");

            Document starDocument = new Document("_id", newStarId)
                    .append("name", starName)
                    .append("birth_year", birthYear)
                    .append("movies", new ArrayList<Document>());

            starsCollection.insertOne(starDocument);

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty(
                    "message",
                    "Star inserted successfully. New star ID: " + newStarId
            );
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }

    private void handleMetadata(HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();

        try {
            JsonArray collectionsJsonArray = new JsonArray();
            MongoIterable<String> collectionNames = database.listCollectionNames();

            for (String collectionName : collectionNames) {
                JsonObject collectionJsonObject = new JsonObject();
                collectionJsonObject.addProperty("table_name", collectionName);

                JsonArray fieldsJsonArray = new JsonArray();
                Document sampleDocument = database.getCollection(collectionName).find().first();

                if (sampleDocument != null) {
                    for (Map.Entry<String, Object> entry : sampleDocument.entrySet()) {
                        JsonObject fieldJsonObject = new JsonObject();
                        fieldJsonObject.addProperty("column_name", entry.getKey());
                        fieldJsonObject.addProperty("column_type", getMongoType(entry.getValue()));
                        fieldsJsonArray.add(fieldJsonObject);
                    }
                }

                collectionJsonObject.add("columns", fieldsJsonArray);
                collectionsJsonArray.add(collectionJsonObject);
            }

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.add("tables", collectionsJsonArray);
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }

    private void handleAddMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();

        String movieTitle = clean(request.getParameter("movie_title"));
        String movieYearString = clean(request.getParameter("movie_year"));
        String movieDirector = clean(request.getParameter("movie_director"));
        String starName = clean(request.getParameter("movie_star_name"));
        String starBirthYearString = clean(request.getParameter("movie_star_birth_year"));
        String genreName = clean(request.getParameter("movie_genre_name"));

        if (movieTitle.isEmpty() || movieYearString.isEmpty() || movieDirector.isEmpty()
                || starName.isEmpty() || genreName.isEmpty()) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Please fill in all required fields.");
            writeJson(response, responseJsonObject);
            return;
        }

        Integer movieYear = parseRequiredYear(movieYearString, "Movie year", responseJsonObject);
        if (responseJsonObject.has("status")) {
            writeJson(response, responseJsonObject);
            return;
        }

        Integer starBirthYear = parseOptionalYear(starBirthYearString, "Star birth year", responseJsonObject);
        if (responseJsonObject.has("status")) {
            writeJson(response, responseJsonObject);
            return;
        }

        try {
            Document existingMovie = moviesCollection.find(Filters.and(
                    Filters.eq("title", movieTitle),
                    Filters.eq("year", movieYear),
                    Filters.eq("director", movieDirector)
            )).first();

            if (existingMovie != null) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Movie already exists.");
                writeJson(response, responseJsonObject);
                return;
            }

            Document starDocument = starsCollection.find(Filters.eq("name", starName)).first();
            String starId;
            Integer finalStarBirthYear = starBirthYear;

            if (starDocument == null) {
                starId = generateNewPrefixedId(starsCollection, "DS");

                Document newStarDocument = new Document("_id", starId)
                        .append("name", starName)
                        .append("birth_year", starBirthYear)
                        .append("movies", new ArrayList<Document>());

                starsCollection.insertOne(newStarDocument);
            } else {
                starId = starDocument.getString("_id");
                finalStarBirthYear = starDocument.getInteger("birth_year");
            }

            Document genreDocument = genresCollection.find(Filters.eq("name", genreName)).first();
            int genreId;

            if (genreDocument == null) {
                genreId = generateNewGenreId();

                Document newGenreDocument = new Document("_id", genreId)
                        .append("name", genreName);

                genresCollection.insertOne(newGenreDocument);
            } else {
                genreId = genreDocument.getInteger("_id");
            }

            String movieId = generateNewPrefixedId(moviesCollection, "DM");

            Document embeddedGenre = new Document("genre_id", genreId)
                    .append("name", genreName);

            Document embeddedStar = new Document("star_id", starId)
                    .append("name", starName)
                    .append("birth_year", finalStarBirthYear);

            Document ratingDocument = new Document("rating", null)
                    .append("vote_count", null);

            Document movieDocument = new Document("_id", movieId)
                    .append("title", movieTitle)
                    .append("year", movieYear)
                    .append("director", movieDirector)
                    .append("rating", ratingDocument)
                    .append("genres", List.of(embeddedGenre))
                    .append("stars", List.of(embeddedStar));

            moviesCollection.insertOne(movieDocument);

            Document starMovieDocument = new Document("movie_id", movieId)
                    .append("title", movieTitle)
                    .append("year", movieYear)
                    .append("director", movieDirector);

            starsCollection.updateOne(
                    Filters.eq("_id", starId),
                    Updates.addToSet("movies", starMovieDocument)
            );

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty(
                    "message",
                    "Movie inserted successfully. Movie ID: " + movieId
                            + ", Star ID: " + starId
                            + ", Genre ID: " + genreId
            );
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer parseRequiredYear(String value, String fieldName, JsonObject responseJsonObject) {
        try {
            int year = Integer.parseInt(value);

            if (year < 0) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", fieldName + " cannot be negative.");
                return null;
            }

            return year;
        } catch (NumberFormatException e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", fieldName + " must be a valid number.");
            return null;
        }
    }

    private Integer parseOptionalYear(String value, String fieldName, JsonObject responseJsonObject) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return parseRequiredYear(value, fieldName, responseJsonObject);
    }

    private String generateNewPrefixedId(MongoCollection<Document> collection, String prefix) {
        int maxNumber = 0;

        FindIterable<Document> documents = collection.find(
                Filters.regex("_id", "^" + prefix + "\\d{7}$")
        ).projection(new Document("_id", 1));

        for (Document document : documents) {
            String id = document.getString("_id");

            if (id != null && id.length() > prefix.length()) {
                try {
                    int number = Integer.parseInt(id.substring(prefix.length()));
                    maxNumber = Math.max(maxNumber, number);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return String.format("%s%07d", prefix, maxNumber + 1);
    }

    private int generateNewGenreId() {
        Document maxGenre = genresCollection.find()
                .sort(new Document("_id", -1))
                .projection(new Document("_id", 1))
                .first();

        if (maxGenre == null) {
            return 1;
        }

        return maxGenre.getInteger("_id") + 1;
    }

    private String getMongoType(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "string";
        }

        if (value instanceof Integer) {
            return "int";
        }

        if (value instanceof Long) {
            return "long";
        }

        if (value instanceof Double || value instanceof Float) {
            return "double";
        }

        if (value instanceof Boolean) {
            return "boolean";
        }

        if (value instanceof Document) {
            return "document";
        }

        if (value instanceof List<?>) {
            return "array";
        }

        return value.getClass().getSimpleName();
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "fail");
        responseJsonObject.addProperty("message", message);
        writeJson(response, responseJsonObject);
    }

    private void writeJson(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();
        out.write(jsonObject.toString());
        out.close();
    }
}