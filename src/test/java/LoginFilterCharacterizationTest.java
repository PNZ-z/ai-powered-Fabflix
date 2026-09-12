import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginFilterCharacterizationTest {

    private LoginFilter filter;

    @BeforeEach
    void initializeFilter() {
        filter = new LoginFilter();
        filter.init(mock(FilterConfig.class));
    }

    @Test
    void allowsLoginApiWithoutSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/fablix/api/login");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).getSession();
    }

    @Test
    void redirectsUnauthenticatedCustomerToLoginPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/fablix/main.html");
        when(request.getContextPath()).thenReturn("/fablix");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/fablix/login.html");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void redirectsUnauthenticatedEmployeeToDashboardLogin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/fablix/api/dashboard/metadata");
        when(request.getContextPath()).thenReturn("/fablix");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("employee")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/fablix/_dashboard.html");
        verify(chain, never()).doFilter(request, response);
    }
}
