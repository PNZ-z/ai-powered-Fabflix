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
import jakarta.servlet.http.HttpSession;
import org.bson.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";

    private MongoClient mongoClient;
    private MongoCollection<Document> customersCollection;
    private MongoCollection<Document> creditCardsCollection;
    private MongoCollection<Document> salesCollection;

    @Override
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);

        customersCollection = database.getCollection("customers");
        creditCardsCollection = database.getCollection("credit_cards");
        salesCollection = database.getCollection("sales");
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();
        response.setContentType("application/json");

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditCardNumber = request.getParameter("creditCardNumber");
        String expirationDate = request.getParameter("expirationDate");

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Customer is not logged in.");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        String email = user.getUsername();

        @SuppressWarnings("unchecked")
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");

        if (cart == null || cart.isEmpty()) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Your shopping cart is empty.");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        try {
            Document creditCard = creditCardsCollection.find(Filters.and(
                    Filters.eq("_id", creditCardNumber),
                    Filters.eq("first_name", firstName),
                    Filters.eq("last_name", lastName),
                    Filters.eq("expiration", expirationDate)
            )).first();

            if (creditCard == null) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid payment information.");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            Document customer = customersCollection.find(Filters.eq("email", email))
                    .projection(new Document("_id", 1))
                    .first();

            if (customer == null) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Customer not found.");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            Integer customerId = customer.getInteger("_id");

            List<Document> saleDocuments = new ArrayList<>();
            String saleDate = LocalDate.now().toString();

            int nextSaleId = generateNextSaleId();

            for (String movieId : cart.keySet()) {
                int quantity = cart.get(movieId);

                for (int i = 0; i < quantity; i++) {
                    Document saleDocument = new Document("_id", nextSaleId++)
                            .append("customer_id", customerId)
                            .append("movie_id", movieId)
                            .append("sale_date", saleDate);

                    saleDocuments.add(saleDocument);
                }
            }

            if (!saleDocuments.isEmpty()) {
                salesCollection.insertMany(saleDocuments);
            }

            cart.clear();

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("message", "Order placed successfully.");
            response.getWriter().write(responseJsonObject.toString());
        } catch (Exception e) {
            response.setStatus(500);
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            response.getWriter().write(responseJsonObject.toString());
        }
    }

    private int generateNextSaleId() {
        Document maxSale = salesCollection.find()
                .sort(new Document("_id", -1))
                .projection(new Document("_id", 1))
                .first();

        if (maxSale == null) {
            return 1;
        }

        return maxSale.getInteger("_id") + 1;
    }
}