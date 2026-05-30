import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

public class CreateMongoIndexes {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=moviedb_mongo";

    private static final String MONGO_DB_NAME = "moviedb_mongo";

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
            MongoDatabase mongoDatabase = mongoClient.getDatabase(MONGO_DB_NAME);

            MongoCollection<Document> moviesCollection =
                    mongoDatabase.getCollection("movies");

            String indexName = moviesCollection.createIndex(
                    Indexes.text("title"),
                    new IndexOptions().name("movies_title_text_index")
            );

            System.out.println("Created index: " + indexName);
        }

        System.out.println("MongoDB indexes created successfully.");
    }
}