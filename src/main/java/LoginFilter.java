import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final List<String> allowedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        System.out.println("LoginFilter: " + requestURI);

        // 1. Allow login pages, login JS, login APIs, and static files without login
        if (this.isUrlAllowedWithoutLogin(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Dashboard pages/APIs require employee login
        if (this.isDashboardUrl(requestURI)) {
            if (httpRequest.getSession().getAttribute("employee") == null) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard.html");
            } else {
                chain.doFilter(request, response);
            }
            return;
        }

        // 3. Everything else requires normal customer login
        if (httpRequest.getSession().getAttribute("user") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    private boolean isDashboardUrl(String requestURI) {
        String lowerCaseURI = requestURI.toLowerCase();

        return lowerCaseURI.endsWith("dashboard.html")
                || lowerCaseURI.endsWith("dashboard.js")
                || lowerCaseURI.endsWith("/api/dashboard")
                || lowerCaseURI.contains("/api/dashboard/");
    }

    public void init(FilterConfig fConfig) {
        // Customer login
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");

        // Employee dashboard login
        allowedURIs.add("_dashboard.html");
        allowedURIs.add("_dashboard.js");
        allowedURIs.add("api/employee-login");

        // Static files
        allowedURIs.add("styles.css");
        allowedURIs.add(".css");
        allowedURIs.add(".js");
        allowedURIs.add(".png");
        allowedURIs.add(".jpg");
        allowedURIs.add(".jpeg");
        allowedURIs.add(".gif");
    }
}