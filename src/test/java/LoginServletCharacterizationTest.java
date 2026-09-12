import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServletCharacterizationTest {

    @Test
    void rejectsLoginWhenRecaptchaVerificationFails() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(request.getParameter("g-recaptcha-response")).thenReturn("invalid-token");
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        try (MockedStatic<RecaptchaVerifyUtils> recaptcha = mockStatic(RecaptchaVerifyUtils.class)) {
            recaptcha.when(() -> RecaptchaVerifyUtils.verify("invalid-token")).thenReturn(false);

            new LoginServlet().doPost(request, response);
        }

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("fail", json.get("status").getAsString());
        assertEquals("Recaptcha Not Passed", json.get("message").getAsString());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
