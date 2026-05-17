import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bson.Document;
import com.mongodb.client.FindIterable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/main")
public class MainServlet extends HttpServlet {

    // 1. Declare the DataSource

    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";
    private static final String GENRES_COLLECTION = "genres";

    private MongoClient mongoClient;
    private MongoCollection<Document> genresCollection;
    // 2. Add the init() method to initialize the connection pool
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        genresCollection = database.getCollection(GENRES_COLLECTION);
    }
    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

//        String genreQuery =
//                "SELECT name " +
//                        "FROM genres " +
//                        "ORDER BY name ASC";

        try {
            // 3. Connect using dataSource instead of DriverManager inside try-with-resources
            JsonArray jsonArray = new JsonArray();
            FindIterable<Document> genres = genresCollection.find().projection(new Document("_id", 0).append("name", 1)).sort(new Document("name", 1));
            for (Document genre : genres){
                JsonObject jsonGenre = new JsonObject();
                jsonGenre.addProperty("name", genre.getString("name"));
                jsonArray.add(jsonGenre);
            }


                out.write(jsonArray.toString());

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}