package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@Import({CommerceApiExceptionHandler.class, CheckoutTokenService.class})
class CheckoutControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void issuesCheckoutTokenAndStoresPendingState() throws Exception {
        when(cartService.getOrCreateCart(any())).thenReturn(new HashMap<>());
        MockHttpSession session = session();

        mockMvc.perform(get("/api/boot/checkout/token").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertNotNull(session.getAttribute(CheckoutTokenService.TOKEN_SESSION_KEY));
        assertEquals("pending", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
    }

    @Test
    void returnsTheStoredOrderEvenWhileCheckoutIsPending() throws Exception {
        MockHttpSession session = session();
        session.setAttribute(CheckoutTokenService.ORDER_SESSION_KEY, sampleOrder());
        // R2-N2: a pending checkout still surfaces the most recent successful order.
        session.setAttribute(CheckoutTokenService.STATE_SESSION_KEY, "pending");

        mockMvc.perform(get("/api/boot/confirmation").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_date").value("2026-10-14"))
                .andExpect(jsonPath("$.total_amount").value(15.0))
                .andExpect(jsonPath("$.lines[0].movie_id").value("tt0001"))
                .andExpect(jsonPath("$.lines[0].movie_title").value("Example"))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].unit_price").value(7.5))
                .andExpect(jsonPath("$.lines[0].line_total").value(15.0));
    }

    @Test
    void returnsNoContentWithoutStoredOrder() throws Exception {
        mockMvc.perform(get("/api/boot/confirmation").session(session()))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/boot/checkout/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Authentication required."));
    }

    @Test
    void mapsInvalidCartRequestsToBadRequest() throws Exception {
        when(cartService.getOrCreateCart(any()))
                .thenThrow(new IllegalArgumentException("Missing movieId or action"));

        mockMvc.perform(get("/api/boot/checkout/token").session(session()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Missing movieId or action"));
    }

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        return session;
    }

    private OrderSummary sampleOrder() {
        return new OrderSummary("2026-10-14", new BigDecimal("15.00"), List.of(
                new OrderLine("tt0001", "Example", 2, new BigDecimal("7.50"), new BigDecimal("15.00"))));
    }
}
