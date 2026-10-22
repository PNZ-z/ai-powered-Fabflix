package com.fabflix.boot.auth;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerAuthController.class)
@Import(AuthApiExceptionHandler.class)
class CustomerAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerAuthService customerAuthService;

    @MockitoBean
    private RecaptchaVerifier recaptchaVerifier;

    @Test
    void storesAuthenticatedCustomerInSession() throws Exception {
        AuthenticatedCustomer customer = new AuthenticatedCustomer("viewer@example.com");
        MockHttpSession session = new MockHttpSession();
        String originalSessionId = session.getId();
        when(recaptchaVerifier.verify("valid-token")).thenReturn(true);
        when(customerAuthService.authenticate("viewer@example.com", "correct-password"))
                .thenReturn(Optional.of(customer));

        mockMvc.perform(post("/api/boot/login")
                        .param("username", "viewer@example.com")
                        .param("password", "correct-password")
                        .param("g-recaptcha-response", "valid-token")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(request -> assertSame(
                        customer,
                        request.getRequest().getSession(false).getAttribute("user")))
                .andExpect(request -> assertNotEquals(
                        originalSessionId,
                        request.getRequest().getSession(false).getId()));
    }

    @Test
    void rejectsFailedRecaptchaBeforeCheckingCredentials() throws Exception {
        when(recaptchaVerifier.verify("invalid-token")).thenReturn(false);

        mockMvc.perform(post("/api/boot/login")
                        .param("username", "viewer@example.com")
                        .param("password", "password")
                        .param("g-recaptcha-response", "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Recaptcha Not Passed"));

        verify(customerAuthService, org.mockito.Mockito.never())
                .authenticate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void preservesLegacyInvalidCredentialResponse() throws Exception {
        when(recaptchaVerifier.verify("valid-token")).thenReturn(true);
        when(customerAuthService.authenticate("viewer@example.com", "wrong-password"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/boot/login")
                        .param("username", "viewer@example.com")
                        .param("password", "wrong-password")
                        .param("g-recaptcha-response", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void invalidatesSessionAndRedirectsOnLogout() throws Exception {
        mockMvc.perform(get("/api/boot/logout")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(request -> assertNull(request.getRequest().getSession(false)));
    }

    @Test
    void returnsNoContentForReactLogout() throws Exception {
        mockMvc.perform(get("/api/boot/logout")
                        .header("Accept", "application/json")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isNoContent())
                .andExpect(request -> assertNull(request.getRequest().getSession(false)));
    }

    @Test
    void returnsSafeErrorWhenAuthenticationDependencyFails() throws Exception {
        when(recaptchaVerifier.verify("valid-token"))
                .thenThrow(new RuntimeException("secret upstream details"));

        mockMvc.perform(post("/api/boot/login")
                        .param("username", "viewer@example.com")
                        .param("password", "password")
                        .param("g-recaptcha-response", "valid-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Authentication service unavailable."));
    }
}
