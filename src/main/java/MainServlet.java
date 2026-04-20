import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/main")
public class MainServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String loginUser = "root";
        String loginPasswd = "20041113";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String genreQuery =
                "SELECT name " +
                        "FROM genres " +
                        "ORDER BY name ASC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (
                    Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
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