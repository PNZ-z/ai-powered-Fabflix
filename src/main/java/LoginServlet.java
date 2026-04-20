import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        /* This example only allows username/password to be test/test
        /  in the real project, you should talk to the database to verify username/password
        */
        JsonObject responseJsonObject = new JsonObject();

//        // Login success:
//        if (username.equals("anteater") && password.equals("123456")) {
//            // Add this user into the session
//            request.getSession().setAttribute("user", new User(username));
//            responseJsonObject.addProperty("status", "success");
//            responseJsonObject.addProperty("message", "success");
//        } else { // Login fail
//            responseJsonObject.addProperty("status", "fail");
//            // Log to localhost log
//            request.getServletContext().log("Login failed");
//            // sample error messages. in practice, it is not a good idea to tell user which one is incorrect/not exist.
//            if (!username.equals("anteater")) {
//                responseJsonObject.addProperty("message", "user " + username + " doesn't exist");
//            } else {
//                responseJsonObject.addProperty("message", "incorrect password");
//            }
//        }

        String loginUser = "root";
        String loginPasswd = "20041113";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        String loginQuery = "SELECT * FROM customers WHERE email= ? and password= ? LIMIT 1";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                    Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
                    PreparedStatement loginStatement = conn.prepareStatement(loginQuery);

            ) {

                JsonObject jsonObject = new JsonObject();
                loginStatement.setString(1, username);
                loginStatement.setString(2, password);
                try(ResultSet loginRs = loginStatement.executeQuery()){
                    if(loginRs.next()){
                        request.getSession().setAttribute("user", new User(username));
//                        int id = loginRs.getInt("id");
//                        String firstName = loginRs.getString("first_name");
//                        String lastName = loginRs.getString("last_name");
//                        String creditCardId = loginRs.getString("credit_card_id");
//                        String email = loginRs.getString("email");
//
//                        jsonObject.addProperty("id", id);
//                        jsonObject.addProperty("first_name", firstName);
//                        jsonObject.addProperty("last_name", lastName);
//                        jsonObject.addProperty("credit_card_id", creditCardId);
//                        jsonObject.addProperty("email", email);
                        jsonObject.addProperty("status", "success");
                    }
                    else{
                        jsonObject.addProperty("status", "fail");
                        jsonObject.addProperty("message", "Invalid username or password");
                    }


                }

                response.getWriter().write(jsonObject.toString());

            }
        } catch (Exception e) {
//            request.getServletContext().log("Error in MovieListServlet", e);
//            out.println("<html><body>");
//            out.println("<h2>Error</h2>");
//            out.println("<p>" + e.getMessage() + "</p>");
//            out.println("</body></html>");



            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            response.getWriter().write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);

        } finally {

        }
    }
}
