import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServletCharacterizationTest {

    @Test
    void rejectsPaymentWhenCustomerIsNotLoggedIn() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter responseBody = new StringWriter();

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new PaymentServlet().doPost(request, response);

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("fail", json.get("status").getAsString());
        assertEquals("Customer is not logged in.", json.get("message").getAsString());
        verify(response).setContentType("application/json");
        verify(session, never()).getAttribute("cart");
    }

    @Test
    void rejectsPaymentWhenShoppingCartIsEmpty() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter responseBody = new StringWriter();

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(new User("viewer@example.com"));
        when(session.getAttribute("cart")).thenReturn(Collections.emptyMap());
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new PaymentServlet().doPost(request, response);

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("fail", json.get("status").getAsString());
        assertEquals("Your shopping cart is empty.", json.get("message").getAsString());
    }
}
