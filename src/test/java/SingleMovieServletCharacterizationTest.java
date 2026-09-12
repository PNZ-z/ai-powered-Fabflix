import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SingleMovieServletCharacterizationTest {

    @Test
    void returnsNotFoundContractWhenMovieDoesNotExist() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement movieStatement = mock(PreparedStatement.class);
        ResultSet movieResult = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(movieStatement);
        when(movieStatement.executeQuery()).thenReturn(movieResult);
        when(movieResult.next()).thenReturn(false);
        when(request.getParameter("id")).thenReturn("missing-id");
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        SingleMovieServlet servlet = new SingleMovieServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(request, response);

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("Movie not found.", json.get("errorMessage").getAsString());
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(connection, times(1)).prepareStatement(anyString());
    }

    @Test
    void preservesMovieJsonContractForNullRatingGenresAndStars() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement movieStatement = mock(PreparedStatement.class);
        PreparedStatement starStatement = mock(PreparedStatement.class);
        ResultSet movieResult = mock(ResultSet.class);
        ResultSet starResult = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.contains("FROM movies m") ? movieStatement : starStatement;
        });
        when(movieStatement.executeQuery()).thenReturn(movieResult);
        when(starStatement.executeQuery()).thenReturn(starResult);
        when(movieResult.next()).thenReturn(true);
        when(movieResult.getString("title")).thenReturn("Example Movie");
        when(movieResult.getInt("year")).thenReturn(2001);
        when(movieResult.getString("director")).thenReturn("Example Director");
        when(movieResult.getString("genres")).thenReturn(null);
        when(movieResult.getFloat("rating")).thenReturn(0.0F);
        when(movieResult.wasNull()).thenReturn(true);
        when(starResult.next()).thenReturn(false);
        when(request.getParameter("id")).thenReturn("tt0001");
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        SingleMovieServlet servlet = new SingleMovieServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(request, response);

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("tt0001", json.get("movie_id").getAsString());
        assertEquals("Example Movie", json.get("movie_title").getAsString());
        assertEquals(2001, json.get("movie_year").getAsInt());
        assertEquals("Example Director", json.get("movie_director").getAsString());
        assertEquals("", json.get("movie_genres").getAsString());
        assertEquals("N/A", json.get("movie_rating").getAsString());

        JsonArray stars = json.getAsJsonArray("movie_stars");
        assertEquals(1, stars.size());
        assertEquals("", stars.get(0).getAsJsonObject().get("star_id").getAsString());
        assertEquals("N/A", stars.get(0).getAsJsonObject().get("star_name").getAsString());
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }
}
