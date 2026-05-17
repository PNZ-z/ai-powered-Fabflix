import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Filters;

import java.io.IOException;


import org.bson.Document;
import org.jasypt.util.password.StrongPasswordEncryptor;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {

    // 1. Declare the DataSource
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";
    private static final String CUSTOMERS_COLLECTION = "customers";

    private MongoClient mongoClient;
    private MongoCollection<Document> customersCollection;
    // 2. Add the init() method to initialize the connection pool
    public void init(ServletConfig config) {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        customersCollection = database.getCollection(CUSTOMERS_COLLECTION);
    }
    @Override
    public void destroy(){
        if(mongoClient!= null){
            mongoClient.close();
        }
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        System.out.println("gRecaptchaResponse=" + gRecaptchaResponse);

        if (!RecaptchaVerifyUtils.verify(gRecaptchaResponse)) {
            try  {
                JsonObject jsonObjectRecaptcha = new JsonObject();
                jsonObjectRecaptcha.addProperty("status", "fail");
                jsonObjectRecaptcha.addProperty("message", "Recaptcha Not Passed");
                response.setStatus(403);
                response.getWriter().write(jsonObjectRecaptcha.toString());
                return;
            }catch (Exception e) {
                e.printStackTrace();
                // Write error message JSON object to output
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("errorMessage", e.getMessage());
                response.getWriter().write(jsonObject.toString());

                // Set response status to 500 (Internal Server Error)
                response.setStatus(500);
            }

        }
        response.setContentType("application/json");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

//        String loginQuery = "SELECT id, email, password FROM customers WHERE email = ? LIMIT 1";

        try {
            // 3. Use try-with-resources with dataSource instead of DriverManager
            JsonObject jsonObject = new JsonObject();
            Document customer  = customersCollection.find(Filters.eq("email", username))
                    .projection(new Document("_id",1)
                            .append("email", 1)
                            .append("password", 1)).limit(1).first();
            if(customer == null){
                jsonObject.addProperty("status", "fail");
                jsonObject.addProperty("message", "Invalid username or password");
            }
            else{
                String customerId = customer.get("_id").toString();
                String customerEmail = customer.getString("email");
                String customerPassword = customer.getString("password");
                StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
                if(passwordEncryptor.checkPassword(password, customerPassword)){
                    request.getSession().setAttribute("user", new User(username));
                    jsonObject.addProperty("status", "success");
                }
                else{
                    jsonObject.addProperty("status", "fail");
                    jsonObject.addProperty("message", "Invalid username or password");

                }


//                System.out.println(jsonObject.toString());

            }
            response.getWriter().write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
    }
}