package com.fabflix.boot.commerce;

import java.util.HashMap;
import java.util.Map;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(CommerceApiExceptionHandler.class)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private PaymentService paymentService;

    @Test
    void clearsCartAfterSuccessfulCommittedOrder() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 2);
        when(cartService.getOrCreateCart(any())).thenReturn(cart);
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.SUCCESS);

        mockMvc.perform(paymentRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Order placed successfully."));

        assertTrue(cart.isEmpty());
    }

    @Test
    void preservesCartWhenPaymentIsInvalid() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 1);
        when(cartService.getOrCreateCart(any())).thenReturn(cart);
        when(paymentService.placeOrder(eq("viewer@example.com"), any(), any()))
                .thenReturn(PaymentResult.INVALID_PAYMENT);

        mockMvc.perform(paymentRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Invalid payment information."));

        assertFalse(cart.isEmpty());
    }

    @Test
    void rejectsEmptyCartBeforeReadingPaymentDetails() throws Exception {
        when(cartService.getOrCreateCart(any())).thenReturn(new HashMap<>());

        mockMvc.perform(paymentRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Your shopping cart is empty."));

        verify(paymentService, never()).placeOrder(any(), any(), any());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder paymentRequest() {
        return post("/api/boot/payment")
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("creditCardNumber", "1234")
                .param("expirationDate", "2030-01-01")
                .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com"));
    }
}
