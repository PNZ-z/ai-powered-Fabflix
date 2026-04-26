import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {

    // 1. Declare the DataSource
    private DataSource dataSource;

    // 2. Add the init() method to initialize the connection pool
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String loginQuery = "SELECT * FROM customers WHERE email= ? and password= ? LIMIT 1";

        try {
            // 3. Use try-with-resources with dataSource instead of DriverManager
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement loginStatement = conn.prepareStatement(loginQuery))
            {
                JsonObject jsonObject = new JsonObject();
                loginStatement.setString(1, username);
                loginStatement.setString(2, password);

                try (ResultSet loginRs = loginStatement.executeQuery()) {
                    if (loginRs.next()) {
                        // Add this user into the session
                        request.getSession().setAttribute("user", new User(username));


                        jsonObject.addProperty("status", "success");
                    } else {
                        jsonObject.addProperty("status", "fail");
                        jsonObject.addProperty("message", "Invalid username or password");
                    }
                }
//                System.out.println(jsonObject.toString());
                response.getWriter().write(jsonObject.toString());
            }
        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        }
    }
}