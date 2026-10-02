package com.fabflix.boot.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerAuthenticationFilterTest {
    private final CustomerAuthenticationFilter filter = new CustomerAuthenticationFilter();

    @Test
    void allowsPublicLoginAndHealthEndpoints() throws Exception {
        MockHttpServletResponse loginResponse = filter("/api/boot/login", null);
        MockHttpServletResponse healthResponse = filter("/api/boot/health", null);

        assertEquals(200, loginResponse.getStatus());
        assertEquals(200, healthResponse.getStatus());
    }

    @Test
    void rejectsProtectedEndpointWithoutAuthenticatedCustomer() throws Exception {
        MockHttpServletResponse response = filter("/api/boot/single-movie", null);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"errorMessage\":\"Authentication required.\"}", response.getContentAsString());
    }

    @Test
    void allowsProtectedEndpointWithAuthenticatedCustomer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boot/single-movie");
        request.getSession().setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
    }

    private MockHttpServletResponse filter(String uri, AuthenticatedCustomer customer) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (customer != null) {
            request.getSession().setAttribute("user", customer);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
