import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/jump-back")
public class JumpBackServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        HttpSession session = request.getSession();
        String lastQuery = null;
        if(session != null){
            lastQuery = (String) session.getAttribute("lastMovieQuery");
        }
        if (lastQuery != null && !lastQuery.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/movieList.html?" + lastQuery);
        } else {
            // Fallback: If they somehow click "Go Back" without ever searching,
            // just send them to a default movie list page.
            response.sendRedirect(request.getContextPath() + "/movieList.html?title=&pageSize=10&pageNumber=0");
        }
    }
}
