package com.fabflix.boot.admin;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiExceptionHandlerTest {
    @Test
    void returnsFailurePayloadWhenEmployeeRequestFails() throws Exception {
        AdminService service = mock(AdminService.class);
        when(service.metadata()).thenThrow(new RuntimeException("metadata query failed"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(service))
                .setControllerAdvice(new AdminApiExceptionHandler())
                .build();

        mockMvc.perform(get("/api/boot/dashboard").param("action", "metadata"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Unable to complete the employee request."));
    }
}
