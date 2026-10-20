package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({CommerceApiExceptionHandler.class, CheckoutTokenService.class})
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private PaymentService paymentService;

    private final CheckoutTokenService tokenService = new CheckoutTokenService();

    @Test
    void returnsOrderDataAndClearsCartAfterSuccessfulOrder() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 2);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(cartService.getCart(session)).thenReturn(List.of(
                new CartItem("tt0001", "Example", 2, new BigDecimal("7.50"))));
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Order placed successfully."))
                .andExpect(jsonPath("$.order.order_date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.order.total_amount").value(15.0))
                .andExpect(jsonPath("$.order.lines[0].movie_id").value("tt0001"))
                .andExpect(jsonPath("$.order.lines[0].movie_title").value("Example"))
                .andExpect(jsonPath("$.order.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.order.lines[0].unit_price").value(7.5))
                .andExpect(jsonPath("$.order.lines[0].line_total").value(15.0));

        assertTrue(cart.isEmpty());
        assertEquals("completed", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
    }

    @Test
    void replaysTheSameOrderForARepeatedToken() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 2);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(cartService.getCart(session)).thenReturn(List.of(
                new CartItem("tt0001", "Example", 2, new BigDecimal("7.50"))));
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // The cart is empty now, but the completed replay must win over the
        // empty-cart guard and return the cached order.
        mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Order placed successfully."))
                .andExpect(jsonPath("$.order.total_amount").value(15.0))
                .andExpect(jsonPath("$.order.lines[0].movie_id").value("tt0001"));

        verify(paymentService, times(1)).placeOrder(any(), any(), any());
    }

    @Test
    void placesANewOrderAfterReissuingTheToken() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 2);
        MockHttpSession session = sessionWithCart(cart);
        String firstToken = tokenService.issueToken(session);
        stubCart(session, cart);
        when(cartService.getCart(session))
                .thenReturn(List.of(new CartItem("tt0001", "Example", 2, new BigDecimal("7.50"))))
                .thenReturn(List.of(new CartItem("tt0001", "Example", 3, new BigDecimal("7.50"))));
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest(session, firstToken))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.order.total_amount").value(15.0));

        // A fresh token starts a new order instead of replaying the cached one.
        String secondToken = tokenService.issueToken(session);
        cart.put("tt0001", 3);

        mockMvc.perform(paymentRequest(session, secondToken))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.order.total_amount").value(22.5))
                .andExpect(jsonPath("$.order.lines[0].quantity").value(3));

        verify(paymentService, times(2)).placeOrder(any(), any(), any());
        assertTrue(cart.isEmpty());
    }

    @Test
    void replaysCachedOrderForAnOldTokenEvenAfterRefillingTheCart() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(cartService.getCart(session)).thenReturn(List.of(
                new CartItem("tt0001", "Example", 1, new BigDecimal("7.50"))));
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(jsonPath("$.order.total_amount").value(7.5));

        // Refill the cart without a new token: the completed token replays the
        // cached order and the new cart is never placed.
        cart.put("tt0001", 5);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.order.total_amount").value(7.5));

        verify(paymentService, times(1)).placeOrder(any(), any(), any());
        assertEquals(5, cart.get("tt0001"));
    }

    @Test
    void rejectsMissingCheckoutTokenWithoutPlacingOrder() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        tokenService.issueToken(session);
        stubCart(session, cart);

        mockMvc.perform(post("/api/boot/payment")
                        .param("firstName", "Ada")
                        .param("lastName", "Lovelace")
                        .param("creditCardNumber", "1234")
                        .param("expirationDate", "2030-01-01")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message")
                        .value("Your checkout session has expired. Please try again."));

        verify(paymentService, never()).placeOrder(any(), any(), any());
        assertFalse(cart.isEmpty());
    }

    @Test
    void rejectsMismatchedCheckoutTokenWithoutPlacingOrder() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        tokenService.issueToken(session);
        stubCart(session, cart);

        mockMvc.perform(paymentRequest(session, "not-the-issued-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message")
                        .value("Your checkout session has expired. Please try again."));

        verify(paymentService, never()).placeOrder(any(), any(), any());
    }

    @Test
    void rejectsEmptyCartWithAFreshToken() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Your shopping cart is empty."));

        verify(paymentService, never()).placeOrder(any(), any(), any());
    }

    @Test
    void preservesCartAndPendingStateWhenPaymentIsInvalid() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.INVALID_PAYMENT);

        String body = mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Invalid payment information."))
                .andExpect(jsonPath("$.order").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertFalse(cart.isEmpty());
        assertEquals("pending", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
        assertFalse(body.contains("1234"), "response must not echo the card number");
    }

    @Test
    void preservesCartAndPendingStateWhenCustomerIsMissing() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.CUSTOMER_NOT_FOUND);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Customer not found."));

        assertFalse(cart.isEmpty());
        assertEquals("pending", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
    }

    @Test
    void allowsRetryWithTheSameTokenAfterInvalidPayment() throws Exception {
        Map<String, Integer> cart = cartWith("tt0001", 1);
        MockHttpSession session = sessionWithCart(cart);
        String token = tokenService.issueToken(session);
        stubCart(session, cart);
        when(cartService.getCart(session)).thenReturn(List.of(
                new CartItem("tt0001", "Example", 1, new BigDecimal("7.50"))));
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.INVALID_PAYMENT)
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(jsonPath("$.status").value("fail"));

        mockMvc.perform(paymentRequest(session, token))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.order.lines[0].movie_id").value("tt0001"));

        verify(paymentService, times(2)).placeOrder(any(), any(), any());
        assertTrue(cart.isEmpty());
    }

    private void stubCart(MockHttpSession session, Map<String, Integer> cart) {
        when(cartService.getOrCreateCart(session)).thenReturn(cart);
    }

    private Map<String, Integer> cartWith(String movieId, int quantity) {
        Map<String, Integer> cart = new HashMap<>();
        cart.put(movieId, quantity);
        return cart;
    }

    private MockHttpSession sessionWithCart(Map<String, Integer> cart) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        session.setAttribute("cart", cart);
        return session;
    }

    private MockHttpServletRequestBuilder paymentRequest(MockHttpSession session, String token) {
        return post("/api/boot/payment")
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("creditCardNumber", "1234")
                .param("expirationDate", "2030-01-01")
                .param("checkoutToken", token)
                .session(session);
    }
}
