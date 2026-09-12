import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovieListServletCharacterizationTest {

    @Test
    void usesDefaultPaginationAndSortingAndStoresTheLastQuery() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter responseBody = new StringWriter();
        String queryString = "title=&pageSize=10&pageNumber=0";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(request.getQueryString()).thenReturn(queryString);
        when(request.getParameter("pageSize")).thenReturn("10");
        when(request.getParameter("pageNumber")).thenReturn("0");
        when(request.getSession(true)).thenReturn(session);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        MovieListServlet servlet = new MovieListServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(request, response);

        assertEquals("[]", responseBody.toString());
        verify(statement).setInt(1, 11);
        verify(statement).setInt(2, 0);
        verify(session).setAttribute("lastMovieQuery", queryString);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains(
                "ORDER BY fm.title ASC, fm.rating IS NULL, fm.rating ASC LIMIT ? OFFSET ?"));
    }

    @Test
    void servesRepeatedQueryFromItsInMemoryCache() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpSession session = mock(HttpSession.class);
        String queryString = "title=Alien&pageSize=10&pageNumber=0";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        MovieListServlet servlet = new MovieListServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        invokeMovieList(servlet, queryString, session);
        String secondResponse = invokeMovieList(servlet, queryString, session);

        assertEquals("[]", secondResponse);
        verify(dataSource, times(1)).getConnection();
    }

    private String invokeMovieList(
            MovieListServlet servlet, String queryString, HttpSession session) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(request.getQueryString()).thenReturn(queryString);
        when(request.getParameter("title")).thenReturn("Alien");
        when(request.getParameter("pageSize")).thenReturn("10");
        when(request.getParameter("pageNumber")).thenReturn("0");
        when(request.getSession(true)).thenReturn(session);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        servlet.doGet(request, response);
        return responseBody.toString();
    }
}
