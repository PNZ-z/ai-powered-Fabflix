package com.fabflix.boot.admin;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmployeeControllerTest {
    @Test void loginCreatesEmployeeSession() throws Exception {
        EmployeeService service = mock(EmployeeService.class);
        EmployeeIdentity employee = new EmployeeIdentity("admin@example.com", "Admin");
        when(service.authenticate("admin@example.com", "secret")).thenReturn(Optional.of(employee));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new EmployeeController(service)).build();

        mvc.perform(post("/api/boot/employee-login")
                        .param("username", "admin@example.com").param("password", "secret")
                        .sessionAttr("placeholder", "value"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("success"))
                .andExpect(request().sessionAttribute("employee", employee));
    }

    @Test void invalidLoginPreservesJsonContract() throws Exception {
        EmployeeService service = mock(EmployeeService.class);
        when(service.authenticate(any(), any())).thenReturn(Optional.empty());
        MockMvcBuilders.standaloneSetup(new EmployeeController(service)).build()
                .perform(post("/api/boot/employee-login").param("username", "bad").param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").exists());
    }

    // The standalone MockMvc below must register the employee filter explicitly:
    // bare standaloneSetup leaves the filter chain empty, so the same request
    // would hit the controller instead of returning 401.
    @Test void logoutRemovesEmployeeButKeepsCustomerSession() throws Exception {
        AuthenticatedCustomer customer = new AuthenticatedCustomer("viewer@example.com");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("employee", new EmployeeIdentity("admin@example.com", "Admin"));
        session.setAttribute("user", customer);
        MockMvc mvc = employeeMvc();

        mvc.perform(get("/api/boot/dashboard/logout").session(session))
                .andExpect(status().isNoContent())
                .andExpect(request().sessionAttributeDoesNotExist("employee"))
                .andExpect(request().sessionAttribute("user", customer));

        assertNull(session.getAttribute("employee"));
        assertSame(customer, session.getAttribute("user"));
    }

    @Test void logoutIsRejectedWithoutEmployeeSession() throws Exception {
        employeeMvc().perform(get("/api/boot/dashboard/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Employee authentication required."));
    }

    private static MockMvc employeeMvc() {
        return MockMvcBuilders.standaloneSetup(new EmployeeController(mock(EmployeeService.class)))
                .addFilters(new EmployeeAuthenticationFilter())
                .build();
    }
}
