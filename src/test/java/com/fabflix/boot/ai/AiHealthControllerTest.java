package com.fabflix.boot.ai;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiHealthController.class)
class AiHealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiHealthService aiHealthService;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/boot/ai/health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Authentication required."));

        verifyNoInteractions(aiHealthService);
    }

    @Test
    void reportsDisabledConfiguration() throws Exception {
        when(aiHealthService.health()).thenReturn(response(false, false, "disabled"));

        mockMvc.perform(get("/api/boot/ai/health").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("disabled"))
                .andExpect(jsonPath("$.reachable").value(false))
                .andExpect(jsonPath("$.chatModel").value("qwen2.5:7b"))
                .andExpect(jsonPath("$.embeddingModel").value("bge-m3"));
    }

    @Test
    void reportsReachableEndpoint() throws Exception {
        when(aiHealthService.health()).thenReturn(response(true, true, "ok"));

        mockMvc.perform(get("/api/boot/ai/health").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.reachable").value(true));
    }

    @Test
    void reportsUnreachableEndpointWithOkStatus() throws Exception {
        when(aiHealthService.health()).thenReturn(response(true, false, "unreachable"));

        mockMvc.perform(get("/api/boot/ai/health").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("unreachable"))
                .andExpect(jsonPath("$.reachable").value(false));
    }

    @Test
    void exposesExactlyTheDocumentedFields() throws Exception {
        when(aiHealthService.health()).thenReturn(response(true, true, "ok"));

        mockMvc.perform(get("/api/boot/ai/health").session(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.chatModel").exists())
                .andExpect(jsonPath("$.embeddingModel").exists())
                .andExpect(jsonPath("$.reachable").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.baseUrl").doesNotExist())
                .andExpect(jsonPath("$.host").doesNotExist())
                .andExpect(jsonPath("$.port").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    private static AiHealthResponse response(boolean enabled, boolean reachable, String status) {
        return new AiHealthResponse(enabled, "qwen2.5:7b", "bge-m3", reachable, status);
    }

    private static MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        return session;
    }
}
