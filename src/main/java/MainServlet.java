import com.google.gson.JsonArray;
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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/main")
public class MainServlet extends HttpServlet {

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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String genreQuery =
                "SELECT name " +
                        "FROM genres " +
                        "ORDER BY name ASC";

        try {
            // 3. Connect using dataSource instead of DriverManager inside try-with-resources
            try (
                    Connection conn = dataSource.getConnection();
                    PreparedStatement statement = conn.prepareStatement(genreQuery);
                    ResultSet rs = statement.executeQuery()
            ) {
                JsonArray jsonArray = new JsonArray();

                while (rs.next()) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", rs.getString("name"));
                    jsonArray.add(jsonObject);
                }

                out.write(jsonArray.toString());
            }
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