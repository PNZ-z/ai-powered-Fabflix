import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FullTextSearchServletCharacterizationTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "!!!"})
    void returnsEmptyJsonArrayWithoutQueryingDatabaseForBlankSearch(String query) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter responseBody = new StringWriter();

        when(request.getQueryString()).thenReturn("query=");
        when(request.getParameter("query")).thenReturn(query);
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody));

        new FullTextSearchServlet().doGet(request, response);

        assertEquals("[]", responseBody.toString());
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "-1", "2147483647"})
    void recognizesValuesAcceptedByIntegerParsing(String value) {
        assertTrue(FullTextSearchServlet.isInteger(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1.5", "one", "2147483648"})
    void rejectsValuesNotAcceptedByIntegerParsing(String value) {
        assertFalse(FullTextSearchServlet.isInteger(value));
    }

    @Test
    void recordsCurrentBehaviorForExplicitPlusSign() {
        assertTrue(FullTextSearchServlet.isInteger("+1"));
    }
}
