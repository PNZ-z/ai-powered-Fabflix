import com.google.gson.JsonObject;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.fabflix.boot.auth.CustomerIdentity;
import java.util.Map;

@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {

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
            CustomerIdentity user = (CustomerIdentity) session.getAttribute("user");

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

        String checkCardQuery =
                "SELECT * FROM credit_cards " +
                        "WHERE id = ? AND first_name = ? AND last_name = ? AND expiration = ?";

        String getCustomerIdQuery =
                "SELECT id FROM customers WHERE email = ?";

        String insertSaleQuery =
                "INSERT INTO sales (customer_id, movie_id, sale_date) " +
                        "VALUES (?, ?, CURDATE())";

        // 3. Use try-with-resources for the database connection
        try (Connection conn = dataSource.getConnection()) {

            try (PreparedStatement checkCardStatement = conn.prepareStatement(checkCardQuery)) {
                checkCardStatement.setString(1, creditCardNumber);
                checkCardStatement.setString(2, firstName);
                checkCardStatement.setString(3, lastName);
                checkCardStatement.setString(4, expirationDate);

                try (ResultSet cardRs = checkCardStatement.executeQuery()) {
                    if (!cardRs.next()) {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Invalid payment information.");
                        response.getWriter().write(responseJsonObject.toString());
                        // try-with-resources auto-closes ResultSet, Statement, and Connection here!
                        return;
                    }
                }
            }

            try (PreparedStatement customerStatement = conn.prepareStatement(getCustomerIdQuery)) {
                customerStatement.setString(1, email);

                try (ResultSet customerRs = customerStatement.executeQuery()) {
                    if (!customerRs.next()) {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Customer not found.");
                        response.getWriter().write(responseJsonObject.toString());
                        return;
                    }

                    int customerId = customerRs.getInt("id");

                    try (PreparedStatement insertSaleStatement = conn.prepareStatement(insertSaleQuery)) {
                        for (String movieId : cart.keySet()) {
                            int quantity = cart.get(movieId);
                            for (int i = 0; i < quantity; i++) {
                                insertSaleStatement.setInt(1, customerId);
                                insertSaleStatement.setString(2, movieId);
                                insertSaleStatement.executeUpdate();
                            }
                        }
                    }
                }
            }

            // If everything succeeded, clear cart and return success
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
}
