import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoverageBranchesCharacterizationTest {

    @Test
    void movieListAppliesEveryFilterAndDescendingSort() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter body = new StringWriter();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(request.getQueryString()).thenReturn("filtered=true");
        when(request.getParameter("title")).thenReturn(" Alien ");
        when(request.getParameter("year")).thenReturn("1979");
        when(request.getParameter("director")).thenReturn("Scott");
        when(request.getParameter("star")).thenReturn("Weaver");
        when(request.getParameter("genre")).thenReturn("Horror");
        when(request.getParameter("startsWith")).thenReturn("A");
        when(request.getParameter("pageSize")).thenReturn("20");
        when(request.getParameter("pageNumber")).thenReturn("2");
        when(request.getParameter("sort1")).thenReturn("rating");
        when(request.getParameter("dir1")).thenReturn("DESC");
        when(request.getParameter("sort2")).thenReturn("title");
        when(request.getParameter("dir2")).thenReturn("DESC");
        when(request.getSession(true)).thenReturn(session);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        MovieListServlet servlet = new MovieListServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doGet(request, response);

        assertEquals("[]", body.toString());
        verify(statement).setString(1, "%Alien%");
        verify(statement).setString(2, "1979");
        verify(statement).setString(3, "%Scott%");
        verify(statement).setString(4, "%Weaver%");
        verify(statement).setString(5, "Horror");
        verify(statement).setString(6, "A");
        verify(statement).setInt(7, 21);
        verify(statement).setInt(8, 40);
    }

    @Test
    void employeeLoginRejectsUnknownEmail() throws Exception {
        EmployeeFixture fixture = employeeFixture();
        when(fixture.resultSet.next()).thenReturn(false);

        fixture.servlet.doPost(fixture.request, fixture.response);

        assertEquals("Employee email does not exist.", json(fixture.body).get("message").getAsString());
        verify(fixture.request, never()).getSession();
    }

    @Test
    void employeeLoginCreatesSessionForValidPassword() throws Exception {
        EmployeeFixture fixture = employeeFixture();
        when(fixture.resultSet.next()).thenReturn(true);
        when(fixture.resultSet.getString("password")).thenReturn(
                new StrongPasswordEncryptor().encryptPassword("secret"));
        when(fixture.resultSet.getString("fullname")).thenReturn("Fabflix Admin");
        HttpSession session = mock(HttpSession.class);
        when(fixture.request.getParameter("password")).thenReturn("secret");
        when(fixture.request.getSession()).thenReturn(session);

        fixture.servlet.doPost(fixture.request, fixture.response);

        assertEquals("success", json(fixture.body).get("status").getAsString());
        verify(session).setAttribute("employee", "admin@example.com");
        verify(session).setAttribute("employeeFullname", "Fabflix Admin");
    }

    @Test
    void paymentRejectsInvalidCardBeforeCustomerLookup() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter body = new StringWriter();
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(new User("viewer@example.com"));
        when(session.getAttribute("cart")).thenReturn(Map.of("tt0001", 1));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        PaymentServlet servlet = new PaymentServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);

        servlet.doPost(request, response);

        JsonObject responseJson = json(body);
        assertEquals("fail", responseJson.get("status").getAsString());
        assertEquals("Invalid payment information.", responseJson.get("message").getAsString());
    }

    private EmployeeFixture employeeFixture() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(request.getParameter("username")).thenReturn("admin@example.com");
        when(response.getWriter()).thenReturn(new PrintWriter(body));
        EmployeeLoginServlet servlet = new EmployeeLoginServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", dataSource);
        return new EmployeeFixture(servlet, request, response, resultSet, body);
    }

    private JsonObject json(StringWriter body) {
        return JsonParser.parseString(body.toString()).getAsJsonObject();
    }

    private static final class EmployeeFixture {
        private final EmployeeLoginServlet servlet;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final ResultSet resultSet;
        private final StringWriter body;

        private EmployeeFixture(EmployeeLoginServlet servlet, HttpServletRequest request,
                HttpServletResponse response, ResultSet resultSet, StringWriter body) {
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.resultSet = resultSet;
            this.body = body;
        }
    }
}
