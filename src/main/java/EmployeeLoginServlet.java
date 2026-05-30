import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/employee-login")
public class EmployeeLoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT email, password, fullname FROM employees WHERE email = ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, email);

                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Employee email does not exist.");
                    } else {
                        String encryptedPassword = rs.getString("password");
                        String fullname = rs.getString("fullname");

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