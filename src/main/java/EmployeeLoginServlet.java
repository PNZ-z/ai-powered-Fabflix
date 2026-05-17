import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/employee-login")
public class EmployeeLoginServlet extends HttpServlet {
    private static final String MONGO_URI =
            "mongodb://testuser:MyPassword6@localhost:27017/?authSource=myNewDB";
    private static final String MONGO_DB_NAME = "moviedb_mongo";
    private static final String EMPLOYEES_COLLECTION = "employees";

    private MongoClient mongoClient;
    private MongoCollection<Document> employeesCollection;

    @Override
    public void init() throws ServletException {
        mongoClient = MongoClients.create(MONGO_URI);
        MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
        employeesCollection = database.getCollection(EMPLOYEES_COLLECTION);
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            Document employee = employeesCollection.find(Filters.eq("_id", email))
                    .projection(new Document("_id", 1)
                            .append("password", 1)
                            .append("fullname", 1))
                    .first();

            if (employee == null) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Employee email does not exist.");
            } else {
                String encryptedPassword = employee.getString("password");
                String fullname = employee.getString("fullname");

                PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

                boolean passwordMatches = passwordEncryptor.checkPassword(
                        password,
                        encryptedPassword
                );

                if (passwordMatches) {
                    HttpSession session = request.getSession();
                    session.setAttribute("employee", email);
                    session.setAttribute("employeeFullname", fullname);

                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "Employee login successful.");
                } else {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Incorrect password.");
                }
            }
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        out.write(responseJsonObject.toString());
        out.close();
    }
}