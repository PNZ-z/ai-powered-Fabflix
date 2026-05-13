 import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.sql.*;
 import java.sql.Date;
 import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public class ConvertMongoDB {
    private static final String MYSQL_USER = "mytestuser";
    private static final String MYSQL_PASSWORD = "My6$Password";
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/moviedb";

    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=moviedb_mongo";

    private static final String MONGO_DB_NAME = "moviedb_mongo";

    public static void main(String[] args) throws SQLException {
        try (
                Connection mysqlConnection =
                        DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);

                MongoClient mongoClient = MongoClients.create(MONGO_URI)
        ) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(MONGO_DB_NAME);

            migrateMovies(mysqlConnection, mongoDatabase);
            migrateStars(mysqlConnection, mongoDatabase);
            migrateGenres(mysqlConnection, mongoDatabase);
            migrateCustomers(mysqlConnection, mongoDatabase);
            migrateCreditCards(mysqlConnection, mongoDatabase);
            migrateSales(mysqlConnection, mongoDatabase);
            migrateEmployees(mysqlConnection, mongoDatabase);

            System.out.println("Migration finished successfully.");
        }
    }

    private static void migrateMovies(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> moviesCollection =
                mongoDatabase.getCollection("movies");

        moviesCollection.drop();

        String query = """
                SELECT
                    m.id AS movie_id,
                    m.title,
                    m.year,
                    m.director,

                    r.rating,
                    r.vote_count,

                    g.id AS genre_id,
                    g.name AS genre_name,

                    s.id AS star_id,
                    s.name AS star_name,
                    s.birth_year AS star_birth_year

                FROM movies m
                LEFT JOIN ratings r
                    ON m.id = r.movie_id
                LEFT JOIN genres_in_movies gim
                    ON m.id = gim.movie_id
                LEFT JOIN genres g
                    ON gim.genre_id = g.id
                LEFT JOIN stars_in_movies sim
                    ON m.id = sim.movie_id
                LEFT JOIN stars s
                    ON sim.star_id = s.id
                ORDER BY m.id;
                """;

        Map<String, Document> movieMap = new LinkedHashMap<>();
        Map<String, Set<Integer>> movieGenreIds = new HashMap<>();
        Map<String, Set<String>> movieStarIds = new HashMap<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                String movieId = rs.getString("movie_id");

                Document movieDocument = movieMap.get(movieId);

                if (movieDocument == null) {
                    movieDocument = new Document("_id", movieId)
                            .append("title", rs.getString("title"))
                            .append("year", rs.getInt("year"))
                            .append("director", rs.getString("director"))
                            .append("genres", new ArrayList<Document>())
                            .append("stars", new ArrayList<Document>());

                    float ratingValue = rs.getFloat("rating");

                    Document ratingDocument;

                    if (rs.wasNull()) {
                        ratingDocument = new Document("rating", null)
                                .append("vote_count", null);
                    } else {
                        ratingDocument = new Document("rating", ratingValue)
                                .append("vote_count", rs.getInt("vote_count"));
                    }

                    movieDocument.append("rating", ratingDocument);

                    movieMap.put(movieId, movieDocument);
                    movieGenreIds.put(movieId, new HashSet<>());
                    movieStarIds.put(movieId, new HashSet<>());
                }

                Integer genreId = getNullableInt(rs, "genre_id");

                if (genreId != null && !movieGenreIds.get(movieId).contains(genreId)) {
                    Document genreDocument = new Document("genre_id", genreId)
                            .append("name", rs.getString("genre_name"));

                    movieDocument.getList("genres", Document.class).add(genreDocument);
                    movieGenreIds.get(movieId).add(genreId);
                }

                String starId = rs.getString("star_id");

                if (starId != null && !movieStarIds.get(movieId).contains(starId)) {
                    Integer birthYear = getNullableInt(rs, "star_birth_year");

                    Document starDocument = new Document("star_id", starId)
                            .append("name", rs.getString("star_name"))
                            .append("birth_year", birthYear);

                    movieDocument.getList("stars", Document.class).add(starDocument);
                    movieStarIds.get(movieId).add(starId);
                }
            }
        }

        if (!movieMap.isEmpty()) {
            moviesCollection.insertMany(new ArrayList<>(movieMap.values()));
        }

        System.out.println("Migrated movies: " + movieMap.size());
    }

    private static void migrateStars(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> starsCollection =
                mongoDatabase.getCollection("stars");

        starsCollection.drop();

        String query = """
                SELECT
                    s.id AS star_id,
                    s.name AS star_name,
                    s.birth_year,

                    m.id AS movie_id,
                    m.title,
                    m.year,
                    m.director

                FROM stars s
                LEFT JOIN stars_in_movies sim
                    ON s.id = sim.star_id
                LEFT JOIN movies m
                    ON sim.movie_id = m.id
                ORDER BY s.id;
                """;

        Map<String, Document> starMap = new LinkedHashMap<>();
        Map<String, Set<String>> starMovieIds = new HashMap<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                String starId = rs.getString("star_id");

                Document starDocument = starMap.get(starId);

                if (starDocument == null) {
                    Integer birthYear = getNullableInt(rs, "birth_year");

                    starDocument = new Document("_id", starId)
                            .append("name", rs.getString("star_name"))
                            .append("birth_year", birthYear)
                            .append("movies", new ArrayList<Document>());

                    starMap.put(starId, starDocument);
                    starMovieIds.put(starId, new HashSet<>());
                }

                String movieId = rs.getString("movie_id");

                if (movieId != null && !starMovieIds.get(starId).contains(movieId)) {
                    Document movieDocument = new Document("movie_id", movieId)
                            .append("title", rs.getString("title"))
                            .append("year", rs.getInt("year"))
                            .append("director", rs.getString("director"));

                    starDocument.getList("movies", Document.class).add(movieDocument);
                    starMovieIds.get(starId).add(movieId);
                }
            }
        }

        if (!starMap.isEmpty()) {
            starsCollection.insertMany(new ArrayList<>(starMap.values()));
        }

        System.out.println("Migrated stars: " + starMap.size());
    }

    private static void migrateGenres(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> genresCollection =
                mongoDatabase.getCollection("genres");

        genresCollection.drop();

        String query = """
                SELECT
                    id,
                    name
                FROM genres
                ORDER BY id;
                """;

        List<Document> genreDocuments = new ArrayList<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                Document genreDocument = new Document("_id", rs.getInt("id"))
                        .append("name", rs.getString("name"));

                genreDocuments.add(genreDocument);
            }
        }

        if (!genreDocuments.isEmpty()) {
            genresCollection.insertMany(genreDocuments);
        }

        System.out.println("Migrated genres: " + genreDocuments.size());
    }

    private static void migrateCustomers(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> customersCollection =
                mongoDatabase.getCollection("customers");

        customersCollection.drop();

        String query = """
                SELECT
                    id,
                    first_name,
                    last_name,
                    credit_card_id,
                    address,
                    email,
                    password
                FROM customers
                ORDER BY id;
                """;

        List<Document> customerDocuments = new ArrayList<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                Document customerDocument = new Document("_id", rs.getInt("id"))
                        .append("first_name", rs.getString("first_name"))
                        .append("last_name", rs.getString("last_name"))
                        .append("credit_card_id", rs.getString("credit_card_id"))
                        .append("address", rs.getString("address"))
                        .append("email", rs.getString("email"))
                        .append("password", rs.getString("password"));

                customerDocuments.add(customerDocument);
            }
        }

        if (!customerDocuments.isEmpty()) {
            customersCollection.insertMany(customerDocuments);
        }

        System.out.println("Migrated customers: " + customerDocuments.size());
    }

    private static void migrateCreditCards(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> creditCardsCollection =
                mongoDatabase.getCollection("credit_cards");

        creditCardsCollection.drop();

        String query = """
                SELECT
                    id,
                    first_name,
                    last_name,
                    expiration
                FROM credit_cards
                ORDER BY id;
                """;

        List<Document> creditCardDocuments = new ArrayList<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                Date expirationDate = rs.getDate("expiration");

                Document creditCardDocument = new Document("_id", rs.getString("id"))
                        .append("first_name", rs.getString("first_name"))
                        .append("last_name", rs.getString("last_name"))
                        .append("expiration", expirationDate);

                creditCardDocuments.add(creditCardDocument);
            }
        }

        if (!creditCardDocuments.isEmpty()) {
            creditCardsCollection.insertMany(creditCardDocuments);
        }

        System.out.println("Migrated credit cards: " + creditCardDocuments.size());
    }

    private static void migrateSales(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> salesCollection =
                mongoDatabase.getCollection("sales");

        salesCollection.drop();

        String query = """
                SELECT
                    id,
                    customer_id,
                    movie_id,
                    sale_date
                FROM sales
                ORDER BY id;
                """;

        List<Document> saleDocuments = new ArrayList<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                Date saleDate = rs.getDate("sale_date");

                Document saleDocument = new Document("_id", rs.getInt("id"))
                        .append("customer_id", rs.getInt("customer_id"))
                        .append("movie_id", rs.getString("movie_id"))
                        .append("sale_date", saleDate);

                saleDocuments.add(saleDocument);
            }
        }

        if (!saleDocuments.isEmpty()) {
            salesCollection.insertMany(saleDocuments);
        }

        System.out.println("Migrated sales: " + saleDocuments.size());
    }

    private static void migrateEmployees(
            Connection mysqlConnection,
            MongoDatabase mongoDatabase
    ) throws SQLException {
        MongoCollection<Document> employeesCollection =
                mongoDatabase.getCollection("employees");

        employeesCollection.drop();

        String query = """
            SELECT
                email,
                password,
                fullname
            FROM employees
            ORDER BY email;
            """;

        List<Document> employeeDocuments = new ArrayList<>();

        try (
                Statement statement = mysqlConnection.createStatement();
                ResultSet rs = statement.executeQuery(query)
        ) {
            while (rs.next()) {
                Document employeeDocument = new Document("_id", rs.getString("email"))
                        .append("password", rs.getString("password"))
                        .append("fullname", rs.getString("fullname"));

                employeeDocuments.add(employeeDocument);
            }
        }

        if (!employeeDocuments.isEmpty()) {
            employeesCollection.insertMany(employeeDocuments);
        }

        System.out.println("Migrated employees: " + employeeDocuments.size());
    }

    private static Integer getNullableInt(ResultSet rs, String columnName)
            throws SQLException {
        int value = rs.getInt(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }
}
