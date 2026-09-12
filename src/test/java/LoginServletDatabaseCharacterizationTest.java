import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServletDatabaseCharacterizationTest {

    @Test
    void storesUserInSessionWhenCredentialsAreValid() throws Exception {
        LoginFixture fixture = loginFixture("correct-password");
        String encryptedPassword = new StrongPasswordEncryptor()
                .encryptPassword("correct-password");
        when(fixture.resultSet.getString("password")).thenReturn(encryptedPassword);

        invokeLogin(fixture);

        JsonObject json = JsonParser.parseString(fixture.responseBody.toString()).getAsJsonObject();
        assertEquals("success", json.get("status").getAsString());
        verify(fixture.statement).setString(1, "viewer@example.com");

        ArgumentCaptor<Object> user = ArgumentCaptor.forClass(Object.class);
        verify(fixture.session).setAttribute(org.mockito.ArgumentMatchers.eq("user"), user.capture());
        User authenticatedUser = assertInstanceOf(User.class, user.getValue());
        assertEquals("viewer@example.com", authenticatedUser.getUsername());
    }

    @Test
    void rejectsIncorrectPasswordWithoutCreatingAuthenticatedSession() throws Exception {
        LoginFixture fixture = loginFixture("wrong-password");
        String encryptedPassword = new StrongPasswordEncryptor()
                .encryptPassword("correct-password");
        when(fixture.resultSet.getString("password")).thenReturn(encryptedPassword);

        invokeLogin(fixture);

        JsonObject json = JsonParser.parseString(fixture.responseBody.toString()).getAsJsonObject();
        assertEquals("fail", json.get("status").getAsString());
        assertEquals("Invalid username or password", json.get("message").getAsString());
        verify(fixture.request, never()).getSession();
    }

    private void invokeLogin(LoginFixture fixture) throws Exception {
        LoginServlet servlet = new LoginServlet();
        ServletTestSupport.setPrivateField(servlet, "dataSource", fixture.dataSource);

        try (MockedStatic<RecaptchaVerifyUtils> recaptcha = mockStatic(RecaptchaVerifyUtils.class)) {
            recaptcha.when(() -> RecaptchaVerifyUtils.verify("valid-token")).thenReturn(true);
            servlet.doPost(fixture.request, fixture.response);
        }
    }

    private LoginFixture loginFixture(String submittedPassword) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter responseBody = new StringWriter();

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(
                "SELECT id, email, password FROM customers WHERE email = ? LIMIT 1"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(request.getParameter("g-recaptcha-response")).thenReturn("valid-token");
        when(request.getParameter("username")).thenReturn("viewer@example.com");
        when(request.getParameter("password")).thenReturn(submittedPassword);
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        return new LoginFixture(
                dataSource, statement, resultSet, request, response, session, responseBody);
    }

    private static final class LoginFixture {
        private final DataSource dataSource;
        private final PreparedStatement statement;
        private final ResultSet resultSet;
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final HttpSession session;
        private final StringWriter responseBody;

        private LoginFixture(
                DataSource dataSource,
                PreparedStatement statement,
                ResultSet resultSet,
                HttpServletRequest request,
                HttpServletResponse response,
                HttpSession session,
                StringWriter responseBody) {
            this.dataSource = dataSource;
            this.statement = statement;
            this.resultSet = resultSet;
            this.request = request;
            this.response = response;
            this.session = session;
            this.responseBody = responseBody;
        }
    }
}
