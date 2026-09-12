import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServletCharacterizationTest {

    @Test
    void rejectsRequestWhenMovieIdIsMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(request.getParameter("action")).thenReturn("add");
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new CartServlet().doPost(request, response);

        assertEquals("Missing movieId or action", responseBody.toString());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(request, never()).getSession();
    }

    @Test
    void createsCartAndAddsFirstMovie() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        StringWriter responseBody = new StringWriter();

        when(request.getParameter("movieId")).thenReturn("tt0001");
        when(request.getParameter("action")).thenReturn("add");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("cart")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new CartServlet().doPost(request, response);

        ArgumentCaptor<Map<String, Integer>> cart = mapCaptor();
        verify(session).setAttribute(org.mockito.ArgumentMatchers.eq("cart"), cart.capture());
        assertEquals(1, cart.getValue().get("tt0001"));

        JsonObject json = JsonParser.parseString(responseBody.toString()).getAsJsonObject();
        assertEquals("success", json.get("status").getAsString());
        verify(response).setContentType("application/json");
    }

    @Test
    void decreasingLastItemRemovesMovieFromCart() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 1);
        HttpServletRequest request = cartRequest("tt0001", "decrease", cart);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        new CartServlet().doPost(request, response);

        assertFalse(cart.containsKey("tt0001"));
    }

    @Test
    void unknownActionReturnsBadRequestWithoutChangingCart() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 2);
        HttpServletRequest request = cartRequest("tt0001", "replace", cart);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new CartServlet().doPost(request, response);

        assertEquals(2, cart.get("tt0001"));
        assertEquals("Unknown action", responseBody.toString());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    private HttpServletRequest cartRequest(
            String movieId, String action, Map<String, Integer> cart) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getParameter("movieId")).thenReturn(movieId);
        when(request.getParameter("action")).thenReturn(action);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("cart")).thenReturn(cart);
        return request;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Map<String, Integer>> mapCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
