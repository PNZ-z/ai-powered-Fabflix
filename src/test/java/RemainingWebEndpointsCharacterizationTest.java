import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemainingWebEndpointsCharacterizationTest {

    @Test
    void mainReturnsGenresAsJson() throws Exception {
        JdbcFixture jdbc = jdbcFixture();
        when(jdbc.resultSet.next()).thenReturn(true, true, false);
        when(jdbc.resultSet.getString("name")).thenReturn("Action", "Comedy");
        Endpoint endpoint = endpoint();
        MainServlet servlet = new MainServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", jdbc.dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        JsonArray json = JsonParser.parseString(endpoint.body.toString()).getAsJsonArray();
        assertEquals(2, json.size());
        assertEquals("Action", json.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("Comedy", json.get(1).getAsJsonObject().get("name").getAsString());
    }

    @Test
    void mainReturnsDatabaseFailureAsJson() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("database unavailable"));
        Endpoint endpoint = endpoint();
        MainServlet servlet = new MainServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        assertEquals("database unavailable", object(endpoint).get("errorMessage").getAsString());
        verify(endpoint.response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void autocompleteSkipsDatabaseForShortQuery() throws Exception {
        Endpoint endpoint = endpoint();
        when(endpoint.request.getParameter("query")).thenReturn("ab");

        new AutocompleteServlet().doGet(endpoint.request, endpoint.response);

        assertEquals("[]", endpoint.body.toString());
    }

    @Test
    void autocompleteReturnsMovieSuggestionContract() throws Exception {
        JdbcFixture jdbc = jdbcFixture();
        when(jdbc.resultSet.next()).thenReturn(true, false);
        when(jdbc.resultSet.getString("id")).thenReturn("tt0001");
        when(jdbc.resultSet.getString("title")).thenReturn("Alien");
        Endpoint endpoint = endpoint();
        when(endpoint.request.getParameter("query")).thenReturn("Alien movie");
        AutocompleteServlet servlet = new AutocompleteServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", jdbc.dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        JsonObject suggestion = JsonParser.parseString(endpoint.body.toString())
                .getAsJsonArray().get(0).getAsJsonObject();
        assertEquals("Alien", suggestion.get("value").getAsString());
        assertEquals("tt0001", suggestion.getAsJsonObject("data").get("movie_id").getAsString());
        verify(jdbc.statement).setString(1, "+Alien* +movie*");
    }

    @Test
    void singleStarReturnsNotFoundContract() throws Exception {
        JdbcFixture jdbc = jdbcFixture();
        when(jdbc.resultSet.next()).thenReturn(false);
        Endpoint endpoint = endpoint();
        when(endpoint.request.getParameter("id")).thenReturn("missing-star");
        SingleStarServlet servlet = new SingleStarServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", jdbc.dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        assertEquals("Star not found.", object(endpoint).get("errorMessage").getAsString());
        verify(endpoint.response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void logoutInvalidatesExistingSessionAndRedirects() throws Exception {
        Endpoint endpoint = endpoint();
        HttpSession session = mock(HttpSession.class);
        when(endpoint.request.getSession(false)).thenReturn(session);
        when(endpoint.request.getContextPath()).thenReturn("/fablix");

        new LogoutServlet().doGet(endpoint.request, endpoint.response);

        verify(session).invalidate();
        verify(endpoint.response).sendRedirect("/fablix/login.html");
    }

    @Test
    void logoutWithoutSessionStillRedirects() throws Exception {
        Endpoint endpoint = endpoint();
        when(endpoint.request.getSession(false)).thenReturn(null);
        when(endpoint.request.getContextPath()).thenReturn("/fablix");

        new LogoutServlet().doGet(endpoint.request, endpoint.response);

        verify(endpoint.response).sendRedirect("/fablix/login.html");
    }

    @Test
    void cartGetCreatesEmptySessionCartAndReturnsArray() throws Exception {
        JdbcFixture jdbc = jdbcFixture();
        Endpoint endpoint = endpoint();
        HttpSession session = mock(HttpSession.class);
        when(endpoint.request.getSession()).thenReturn(session);
        when(session.getAttribute("cart")).thenReturn(null);
        CartServlet servlet = new CartServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", jdbc.dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        assertEquals("[]", endpoint.body.toString());
        verify(session).setAttribute(org.mockito.ArgumentMatchers.eq("cart"), any(Map.class));
    }

    @Test
    void dashboardRejectsUnknownActions() throws Exception {
        Endpoint getEndpoint = endpoint();
        when(getEndpoint.request.getParameter("action")).thenReturn("unknown");
        new DashboardServlet().doGet(getEndpoint.request, getEndpoint.response);
        assertEquals("Invalid GET action.", object(getEndpoint).get("message").getAsString());

        Endpoint postEndpoint = endpoint();
        when(postEndpoint.request.getParameter("action")).thenReturn("unknown");
        new DashboardServlet().doPost(postEndpoint.request, postEndpoint.response);
        assertEquals("Invalid POST action.", object(postEndpoint).get("message").getAsString());
    }

    @Test
    void dashboardValidatesStarInputsBeforeDatabaseAccess() throws Exception {
        Endpoint emptyName = endpoint();
        when(emptyName.request.getParameter("action")).thenReturn("add-star");
        when(emptyName.request.getParameter("star_name")).thenReturn(" ");
        new DashboardServlet().doPost(emptyName.request, emptyName.response);
        assertEquals("Star name cannot be empty.", object(emptyName).get("message").getAsString());

        Endpoint badYear = endpoint();
        when(badYear.request.getParameter("action")).thenReturn("add-star");
        when(badYear.request.getParameter("star_name")).thenReturn("New Star");
        when(badYear.request.getParameter("birth_year")).thenReturn("not-a-year");
        new DashboardServlet().doPost(badYear.request, badYear.response);
        assertEquals("Birth year must be a valid number.", object(badYear).get("message").getAsString());

        Endpoint negativeYear = endpoint();
        when(negativeYear.request.getParameter("action")).thenReturn("add-star");
        when(negativeYear.request.getParameter("star_name")).thenReturn("New Star");
        when(negativeYear.request.getParameter("birth_year")).thenReturn("-1");
        new DashboardServlet().doPost(negativeYear.request, negativeYear.response);
        assertEquals("Birth year cannot be negative.", object(negativeYear).get("message").getAsString());
    }

    @Test
    void dashboardValidatesMovieInputsBeforeStoredProcedure() throws Exception {
        Endpoint missing = endpoint();
        when(missing.request.getParameter("action")).thenReturn("add-movie");
        new DashboardServlet().doPost(missing.request, missing.response);
        assertEquals("Please fill in all required fields.", object(missing).get("message").getAsString());

        Endpoint badYear = validMovieEndpoint("not-a-year", null);
        new DashboardServlet().doPost(badYear.request, badYear.response);
        assertEquals("Movie year must be a valid number.", object(badYear).get("message").getAsString());

        Endpoint negativeStarYear = validMovieEndpoint("2001", "-1");
        new DashboardServlet().doPost(negativeStarYear.request, negativeStarYear.response);
        assertEquals("Star birth year cannot be negative.", object(negativeStarYear).get("message").getAsString());
    }

    @Test
    void metadataReturnsTableAndColumnContract() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet tables = mock(ResultSet.class);
        ResultSet columns = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.getCatalog()).thenReturn("moviedb");
        when(metadata.getTables(anyString(), any(), anyString(), any(String[].class))).thenReturn(tables);
        when(metadata.getColumns(anyString(), any(), anyString(), anyString())).thenReturn(columns);
        when(tables.next()).thenReturn(true, false);
        when(tables.getString("TABLE_NAME")).thenReturn("movies");
        when(columns.next()).thenReturn(true, false);
        when(columns.getString("COLUMN_NAME")).thenReturn("title");
        when(columns.getString("TYPE_NAME")).thenReturn("VARCHAR");
        when(columns.getInt("COLUMN_SIZE")).thenReturn(100);
        Endpoint endpoint = endpoint();
        when(endpoint.request.getParameter("action")).thenReturn("metadata");
        DashboardServlet servlet = new DashboardServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(endpoint.request, endpoint.response);

        JsonObject json = object(endpoint);
        assertEquals("success", json.get("status").getAsString());
        assertEquals("VARCHAR(100)", json.getAsJsonArray("tables").get(0).getAsJsonObject()
                .getAsJsonArray("columns").get(0).getAsJsonObject().get("column_type").getAsString());
    }

    private Endpoint validMovieEndpoint(String movieYear, String starYear) throws Exception {
        Endpoint endpoint = endpoint();
        when(endpoint.request.getParameter("action")).thenReturn("add-movie");
        when(endpoint.request.getParameter("movie_title")).thenReturn("Movie");
        when(endpoint.request.getParameter("movie_year")).thenReturn(movieYear);
        when(endpoint.request.getParameter("movie_director")).thenReturn("Director");
        when(endpoint.request.getParameter("movie_star_name")).thenReturn("Star");
        when(endpoint.request.getParameter("movie_star_birth_year")).thenReturn(starYear);
        when(endpoint.request.getParameter("movie_genre_name")).thenReturn("Drama");
        return endpoint;
    }

    private JdbcFixture jdbcFixture() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        return new JdbcFixture(dataSource, statement, resultSet);
    }

    private Endpoint endpoint() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletContext servletContext = mock(ServletContext.class);
        StringWriter body = new StringWriter();
        when(request.getServletContext()).thenReturn(servletContext);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        return new Endpoint(request, response, body);
    }

    private JsonObject object(Endpoint endpoint) {
        return JsonParser.parseString(endpoint.body.toString()).getAsJsonObject();
    }

    private static final class Endpoint {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final StringWriter body;

        private Endpoint(HttpServletRequest request, HttpServletResponse response, StringWriter body) {
            this.request = request;
            this.response = response;
            this.body = body;
        }
    }

    private static final class JdbcFixture {
        private final DataSource dataSource;
        private final PreparedStatement statement;
        private final ResultSet resultSet;

        private JdbcFixture(DataSource dataSource, PreparedStatement statement, ResultSet resultSet) {
            this.dataSource = dataSource;
            this.statement = statement;
            this.resultSet = resultSet;
        }
    }
}
