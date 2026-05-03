import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "DashboardServlet", urlPatterns = "/api/dashboard")
public class DashboardServlet extends HttpServlet {
    private DataSource dataSource;

    public void init() throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        if ("metadata".equals(action)) {
            handleMetadata(response);
        } else {
            writeError(response, "Invalid GET action.");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        if ("add-star".equals(action)) {
            handleAddStar(request, response);
        } else if ("add-movie".equals(action)) {
            handleAddMovie(request, response);
        } else {
            writeError(response, "Invalid POST action.");
        }
    }

    private void handleAddStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JsonObject responseJsonObject = new JsonObject();

        String starName = request.getParameter("star_name");
        String birthYearString = request.getParameter("birth_year");

        if (starName == null || starName.trim().isEmpty()) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Star name cannot be empty.");
            writeJson(response, responseJsonObject);
            return;
        }

        Integer birthYear = null;

        if (birthYearString != null && !birthYearString.trim().isEmpty()) {
            try {
                birthYear = Integer.parseInt(birthYearString);

                if (birthYear < 0) {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Birth year cannot be negative.");
                    writeJson(response, responseJsonObject);
                    return;
                }
            } catch (NumberFormatException e) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Birth year must be a valid number.");
                writeJson(response, responseJsonObject);
                return;
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            String newStarId = generateNewStarId(connection);

            String insertQuery = "INSERT INTO stars (id, name, birth_year) VALUES (?, ?, ?)";

            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                insertStatement.setString(1, newStarId);
                insertStatement.setString(2, starName.trim());

                if (birthYear == null) {
                    insertStatement.setNull(3, Types.INTEGER);
                } else {
                    insertStatement.setInt(3, birthYear);
                }

                int affectedRows = insertStatement.executeUpdate();

                if (affectedRows == 1) {
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty(
                            "message",
                            "Star inserted successfully. New star ID: " + newStarId
                    );
                } else {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Failed to insert star.");
                }
            }
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }

    private void handleMetadata(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JsonObject responseJsonObject = new JsonObject();

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            JsonArray tablesJsonArray = new JsonArray();

            try (ResultSet tables = databaseMetaData.getTables(
                    connection.getCatalog(),
                    null,
                    "%",
                    new String[]{"TABLE"})) {

                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");

                    JsonObject tableJsonObject = new JsonObject();
                    tableJsonObject.addProperty("table_name", tableName);

                    JsonArray columnsJsonArray = new JsonArray();

                    try (ResultSet columns = databaseMetaData.getColumns(
                            connection.getCatalog(),
                            null,
                            tableName,
                            "%")) {

                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            String columnType = columns.getString("TYPE_NAME");
                            int columnSize = columns.getInt("COLUMN_SIZE");

                            JsonObject columnJsonObject = new JsonObject();
                            columnJsonObject.addProperty("column_name", columnName);
                            columnJsonObject.addProperty(
                                    "column_type",
                                    columnType + "(" + columnSize + ")"
                            );

                            columnsJsonArray.add(columnJsonObject);
                        }
                    }

                    tableJsonObject.add("columns", columnsJsonArray);
                    tablesJsonArray.add(tableJsonObject);
                }
            }

            responseJsonObject.addProperty("status", "success");
            responseJsonObject.add("tables", tablesJsonArray);

        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }

    private String generateNewStarId(Connection connection) throws Exception {
        String query = "SELECT MAX(id) AS max_id FROM stars";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                String maxId = rs.getString("max_id");

                if (maxId != null && maxId.length() > 2) {
                    int number = Integer.parseInt(maxId.substring(2));
                    number++;

                    return String.format("nm%07d", number);
                }
            }
        }

        return "nm0000001";
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "fail");
        responseJsonObject.addProperty("message", message);
        writeJson(response, responseJsonObject);
    }

    private void writeJson(HttpServletResponse response, JsonObject jsonObject) throws IOException {
        response.setContentType("application/json");

        PrintWriter out = response.getWriter();
        out.write(jsonObject.toString());
        out.close();
    }

    private void handleAddMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();

        String movieTitle = request.getParameter("movie_title");
        String movieYearString = request.getParameter("movie_year");
        String movieDirector = request.getParameter("movie_director");
        String starName = request.getParameter("movie_star_name");
        String starBirthYearString = request.getParameter("movie_star_birth_year");
        String genreName = request.getParameter("movie_genre_name");

        if (movieTitle == null || movieTitle.trim().isEmpty()
                || movieYearString == null || movieYearString.trim().isEmpty()
                || movieDirector == null || movieDirector.trim().isEmpty()
                || starName == null || starName.trim().isEmpty()
                || genreName == null || genreName.trim().isEmpty()) {

            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Please fill in all required fields.");
            writeJson(response, responseJsonObject);
            return;
        }

        int movieYear;

        try {
            movieYear = Integer.parseInt(movieYearString);

            if (movieYear < 0) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Movie year cannot be negative.");
                writeJson(response, responseJsonObject);
                return;
            }
        } catch (NumberFormatException e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Movie year must be a valid number.");
            writeJson(response, responseJsonObject);
            return;
        }

        Integer starBirthYear = null;

        if (starBirthYearString != null && !starBirthYearString.trim().isEmpty()) {
            try {
                starBirthYear = Integer.parseInt(starBirthYearString);

                if (starBirthYear < 0) {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Star birth year cannot be negative.");
                    writeJson(response, responseJsonObject);
                    return;
                }
            } catch (NumberFormatException e) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Star birth year must be a valid number.");
                writeJson(response, responseJsonObject);
                return;
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            String procedureCall = "{CALL add_movie(?, ?, ?, ?, ?, ?)}";

            try (CallableStatement callableStatement = connection.prepareCall(procedureCall)) {
                callableStatement.setString(1, movieTitle.trim());
                callableStatement.setInt(2, movieYear);
                callableStatement.setString(3, movieDirector.trim());
                callableStatement.setString(4, starName.trim());

                if (starBirthYear == null) {
                    callableStatement.setNull(5, Types.INTEGER);
                } else {
                    callableStatement.setInt(5, starBirthYear);
                }

                callableStatement.setString(6, genreName.trim());

                boolean hasResultSet = callableStatement.execute();

                if (hasResultSet) {
                    try (ResultSet rs = callableStatement.getResultSet()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            String message = rs.getString("message");

                            responseJsonObject.addProperty("status", status);
                            responseJsonObject.addProperty("message", message);
                        } else {
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Stored procedure returned no result.");
                        }
                    }
                } else {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Stored procedure returned no result set.");
                }
            }
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", e.getMessage());
            e.printStackTrace();
        }

        writeJson(response, responseJsonObject);
    }
}