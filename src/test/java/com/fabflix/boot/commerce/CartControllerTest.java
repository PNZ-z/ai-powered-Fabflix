package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.List;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(CommerceApiExceptionHandler.class)
class CartControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    void returnsCartUsingLegacyJsonContract() throws Exception {
        when(cartService.getCart(any())).thenReturn(List.of(
                new CartItem("tt0001", "Example", 2, new BigDecimal("7.50"))
        ));

        mockMvc.perform(get("/api/boot/cart")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movie_id").value("tt0001"))
                .andExpect(jsonPath("$[0].movie_title").value("Example"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].price").value(7.50));
    }

    @Test
    void updatesCartFromFormParameters() throws Exception {
        mockMvc.perform(post("/api/boot/cart")
                        .param("movieId", "tt0001")
                        .param("action", "increase")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(cartService).updateCart(any(), org.mockito.ArgumentMatchers.eq("tt0001"),
                org.mockito.ArgumentMatchers.eq("increase"));
    }

    @Test
    void returnsBadRequestForInvalidAction() throws Exception {
        doThrow(new IllegalArgumentException("Unknown action"))
                .when(cartService).updateCart(any(), any(), any());

        mockMvc.perform(post("/api/boot/cart")
                        .param("movieId", "tt0001")
                        .param("action", "replace")
                        .sessionAttr("user", new AuthenticatedCustomer("viewer@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Unknown action"));
    }
}
