package com.fabflix.boot.session;

import com.fabflix.boot.admin.EmployeeIdentity;
import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SessionController()).build();

    @Test
    void returnsTheAuthenticatedCustomer() throws Exception {
        mvc.perform(get("/api/boot/session/customer")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.role").value("customer"))
                .andExpect(jsonPath("$.username").value("viewer@example.com"))
                .andExpect(jsonPath("$.displayName").doesNotExist());
    }

    @Test
    void returnsTheAuthenticatedEmployee() throws Exception {
        mvc.perform(get("/api/boot/dashboard/session")
                        .sessionAttr("employee", new EmployeeIdentity("admin@example.com", "Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.role").value("employee"))
                .andExpect(jsonPath("$.username").value("admin@example.com"))
                .andExpect(jsonPath("$.displayName").value("Admin"));
    }

    @Test
    void rejectsMissingSessionIdentities() throws Exception {
        mvc.perform(get("/api/boot/session/customer"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/boot/dashboard/session"))
                .andExpect(status().isUnauthorized());
    }
}
