import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JumpBackServletCharacterizationTest {

    @Test
    void redirectsToTheLastMovieQueryStoredInSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("lastMovieQuery"))
                .thenReturn("title=Alien&pageSize=10&pageNumber=0");
        when(request.getContextPath()).thenReturn("/fablix");

        new JumpBackServlet().doGet(request, response);

        verify(response).sendRedirect(
                "/fablix/movieList.html?title=Alien&pageSize=10&pageNumber=0");
    }

    @Test
    void redirectsToDefaultMovieListWhenNoPreviousQueryExists() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("lastMovieQuery")).thenReturn(null);
        when(request.getContextPath()).thenReturn("/fablix");

        new JumpBackServlet().doGet(request, response);

        verify(response).sendRedirect(
                "/fablix/movieList.html?title=&pageSize=10&pageNumber=0");
    }
}
