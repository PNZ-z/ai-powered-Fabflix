package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {
    @Test
    void createsSessionCartAndSupportsAllActions() {
        CartService service = new CartService(mock(CartRepository.class));
        MockHttpSession session = new MockHttpSession();

        service.updateCart(session, " tt0001 ", "add");
        @SuppressWarnings("unchecked")
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        service.updateCart(session, "tt0001", "increase");
        assertEquals(2, cart.get("tt0001"));

        service.updateCart(session, "tt0001", "decrease");
        assertEquals(1, cart.get("tt0001"));

        service.updateCart(session, "tt0001", "decrease");
        assertFalse(cart.containsKey("tt0001"));

        service.updateCart(session, "tt0002", "add");
        service.updateCart(session, "tt0002", "delete");
        assertFalse(cart.containsKey("tt0002"));
    }

    @Test
    void rejectsMissingValuesAndUnknownActionWithoutChangingCart() {
        CartService service = new CartService(mock(CartRepository.class));
        HttpSession session = mock(HttpSession.class);
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 2);
        when(session.getAttribute("cart")).thenReturn(cart);

        assertThrows(IllegalArgumentException.class, () -> service.updateCart(session, null, "add"));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateCart(session, "tt0001", "replace")
        );

        assertEquals("Unknown action", failure.getMessage());
        assertEquals(2, cart.get("tt0001"));
    }

    @Test
    void returnsKnownMoviesInStableOrderWithLegacyPrices() {
        CartRepository repository = mock(CartRepository.class);
        CartService service = new CartService(repository);
        HttpSession session = mock(HttpSession.class);
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0002", 2);
        cart.put("tt0001", 1);
        when(session.getAttribute("cart")).thenReturn(cart);
        when(repository.findMoviesByIds(cart.keySet())).thenReturn(Map.of(
                "tt0001", new MovieCartSummary("tt0001", "First"),
                "tt0002", new MovieCartSummary("tt0002", "Second")
        ));

        List<CartItem> items = service.getCart(session);

        assertEquals(List.of("tt0001", "tt0002"),
                items.stream().map(CartItem::getMovieId).collect(Collectors.toList()));
        assertEquals(1, items.get(0).getQuantity());
        assertEquals(service.priceFor("tt0001"), items.get(0).getPrice());
        assertEquals(2, items.get(1).getQuantity());
    }

    @Test
    void persistsCartChangesAcrossRequestsOnTheSameSession() {
        CartRepository repository = mock(CartRepository.class);
        CartService service = new CartService(repository);
        MockHttpSession session = new MockHttpSession();
        when(repository.findMoviesByIds(any())).thenReturn(Map.of(
                "tt0001", new MovieCartSummary("tt0001", "First")));

        service.updateCart(session, "tt0001", "increase");
        service.updateCart(session, "tt0001", "increase");

        List<CartItem> items = service.getCart(session);
        assertEquals(1, items.size());
        assertEquals(2, items.get(0).getQuantity());

        service.updateCart(session, "tt0001", "delete");
        assertTrue(service.getCart(session).isEmpty());
    }

    @Test
    void priceIsAlwaysWithinLegacyRange() {
        CartService service = new CartService(mock(CartRepository.class));

        BigDecimal price = service.priceFor("polygenelubricants");

        assertEquals(true, price.compareTo(new BigDecimal("5.00")) >= 0);
        assertEquals(true, price.compareTo(new BigDecimal("19.99")) <= 0);
    }
}
