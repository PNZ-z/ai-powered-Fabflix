package com.fabflix.boot.admin;

import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
}
