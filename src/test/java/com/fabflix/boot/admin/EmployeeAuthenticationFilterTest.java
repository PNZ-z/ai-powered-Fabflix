package com.fabflix.boot.admin;

import static org.junit.jupiter.api.Assertions.*;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class EmployeeAuthenticationFilterTest {
    @Test void rejectsDashboardWithoutEmployeeSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boot/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new EmployeeAuthenticationFilter().doFilter(request, response, (req, res) -> fail("must not continue"));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Employee authentication required"));
    }

    @Test void allowsAuthenticatedEmployee() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boot/dashboard");
        request.getSession().setAttribute("employee", new EmployeeIdentity("a@b.com", "Admin"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] called = {false};
        FilterChain chain = (req, res) -> called[0] = true;
        new EmployeeAuthenticationFilter().doFilter(request, response, chain);
        assertTrue(called[0]);
    }
}
