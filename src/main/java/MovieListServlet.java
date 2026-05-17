import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.Arrays;
@WebServlet("/api/movie-list")
public class MovieListServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";
    private static final String MOVIES_COLLECTION = "movies";

    private MongoClient mongoClient;
    private MongoCollection<Document> moviesCollection;

    private final ConcurrentHashMap<String, String> queryCache = new ConcurrentHashMap<>();

    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        moviesCollection = database.getCollection(MOVIES_COLLECTION);
    }

    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String searchTitle = clean(request.getParameter("title"));
        String searchYear = clean(request.getParameter("year"));
        String searchDirector = clean(request.getParameter("director"));
        String searchStar = clean(request.getParameter("star"));
        String searchGenre = clean(request.getParameter("genre"));
        String startsWith = clean(request.getParameter("startsWith"));

        int pageNumber = parsePositiveInt(request.getParameter("pageNumber"), 0);
        int pageSize = parsePositiveInt(request.getParameter("pageSize"), 10);

        String sort1 = clean(request.getParameter("sort1"));
        String sort1Direction = clean(request.getParameter("dir1"));
        String sort2 = clean(request.getParameter("sort2"));
        String sort2Direction = clean(request.getParameter("dir2"));

        if (sort1.isEmpty()) sort1 = "title";
        if (sort2.isEmpty()) sort2 = "rating";
        if (!isValidSortField(sort1)) sort1 = "title";
        if (!isValidSortField(sort2)) sort2 = "rating";

        if (!isValidSortDirection(sort1Direction)) sort1Direction = "ASC";
        if (!isValidSortDirection(sort2Direction)) sort2Direction = "DESC";

        String cacheKey = request.getQueryString();
        if (cacheKey == null) {
            cacheKey = "";
        }

        HttpSession session = request.getSession();
        session.setAttribute("lastMovieQuery", "movieList.html?" + cacheKey);

        String cached = queryCache.get(cacheKey);
        if (cached != null) {
            response.getWriter().write(cached);
            return;
        }

        try {
            List<Bson> filters = new ArrayList<>();

            if (!searchTitle.isEmpty()) {
                filters.add(Filters.regex("title", containsPattern(searchTitle)));
            }

            if (!searchYear.isEmpty()) {
                try {
                    filters.add(Filters.eq("year", Integer.parseInt(searchYear)));
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Year must be a number.");
                    response.getWriter().write(error.toString());
                    return;
                }
            }

            if (!searchDirector.isEmpty()) {
                filters.add(Filters.regex("director", containsPattern(searchDirector)));
            }

            if (!searchStar.isEmpty()) {
                filters.add(Filters.regex("stars.name", containsPattern(searchStar)));
            }

            if (!searchGenre.isEmpty()) {
                filters.add(Filters.eq("genres.name", searchGenre));
            }

            if (!startsWith.isEmpty()) {
                filters.add(Filters.regex("title", startsWithPattern(startsWith)));
            }

            Bson matchFilter = filters.isEmpty() ? new Document() : Filters.and(filters);

            Document addFields = new Document()
                    .append("rating_is_null",
                            new Document("$cond", Arrays.asList(
                                    new Document("$eq", Arrays.asList("$rating.rating", null)),
                                    1,
                                    0
                            )))
                    .append("title_sort", new Document("$toLower", "$title"));

            Document sortDocument = new Document();
            appendSort(sortDocument, sort1, sort1Direction);
            appendSort(sortDocument, sort2, sort2Direction);
            sortDocument.append("_id", 1);

            int offset = pageNumber * pageSize;

            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(matchFilter));
            pipeline.add(new Document("$addFields", addFields));
            pipeline.add(Aggregates.sort(sortDocument));
            pipeline.add(Aggregates.skip(offset));
            pipeline.add(Aggregates.limit(pageSize + 1));

            AggregateIterable<Document> movies = moviesCollection.aggregate(pipeline);

            JsonArray jsonArray = new JsonArray();

            for (Document movie : movies) {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("movie_id", movie.getString("_id"));
                jsonObject.addProperty("movie_title", movie.getString("title"));
                jsonObject.addProperty("movie_year", movie.getInteger("year"));
                jsonObject.addProperty("movie_director", movie.getString("director"));

                jsonObject.addProperty("movie_genres", getGenreText(movie));

                Float rating = getRating(movie);
                if (rating == null) {
                    jsonObject.add("movie_rating", JsonNull.INSTANCE);
                } else {
                    jsonObject.addProperty("movie_rating", rating);
                }

                JsonArray starsJsonArray = getStarsJsonArray(movie);
                jsonObject.add("movie_stars", starsJsonArray);

                jsonArray.add(jsonObject);
            }

            String jsonResponse = jsonArray.toString();
            queryCache.put(cacheKey, jsonResponse);

            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());

            response.getWriter().write(error.toString());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(parsed, 0);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Pattern containsPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
    }

    private Pattern startsWithPattern(String value) {
        return Pattern.compile("^" + Pattern.quote(value), Pattern.CASE_INSENSITIVE);
    }

    private boolean isValidSortField(String value) {
        return "title".equalsIgnoreCase(value) || "rating".equalsIgnoreCase(value);
    }

    private boolean isValidSortDirection(String value) {
        return "ASC".equalsIgnoreCase(value) || "DESC".equalsIgnoreCase(value);
    }

    private void appendSort(Document sortDocument, String sortField, String direction) {
        int sortDirection = "DESC".equalsIgnoreCase(direction) ? -1 : 1;

        if ("rating".equalsIgnoreCase(sortField)) {
            if (!sortDocument.containsKey("rating_is_null")) {
                sortDocument.append("rating_is_null", 1);
            }
            sortDocument.append("rating.rating", sortDirection);
        } else {
            sortDocument.append("title_sort", sortDirection);
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

        if (genreNames.size() > 3) {
            genreNames = genreNames.subList(0, 3);
        }

        return String.join(", ", genreNames);
    }

    private Float getRating(Document movie) {
        Document ratingDocument = movie.get("rating", Document.class);

        if (ratingDocument == null) {
            return null;
        }

        Object ratingValue = ratingDocument.get("rating");

        if (ratingValue == null) {
            return null;
        }

        if (ratingValue instanceof Number) {
            return ((Number) ratingValue).floatValue();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private JsonArray getStarsJsonArray(Document movie) {
        JsonArray starsJsonArray = new JsonArray();

        List<Document> stars = (List<Document>) movie.get("stars");

        if (stars == null || stars.isEmpty()) {
            return starsJsonArray;
        }

        stars.sort(Comparator.comparing(
                star -> {
                    String name = star.getString("name");
                    return name == null ? "" : name.toLowerCase();
                }
        ));

        int limit = Math.min(3, stars.size());

        for (int i = 0; i < limit; i++) {
            Document star = stars.get(i);

            JsonObject starJson = new JsonObject();
            starJson.addProperty("star_id", star.getString("star_id"));
            starJson.addProperty("star_name", star.getString("name"));

            starsJsonArray.add(starJson);
        }

        return starsJsonArray;
    }
}
