package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

@Service
public class CartService {
    static final String CART_SESSION_KEY = "cart";

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public List<CartItem> getCart(HttpSession session) {
        Map<String, Integer> cart = getOrCreateCart(session);
        Map<String, Integer> cartSnapshot;
        synchronized (cart) {
            cartSnapshot = new HashMap<>(cart);
        }

        Map<String, MovieCartSummary> movies = cartRepository.findMoviesByIds(cartSnapshot.keySet());
        List<CartItem> items = new ArrayList<>();
        cartSnapshot.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    MovieCartSummary movie = movies.get(entry.getKey());
                    if (movie != null) {
                        items.add(new CartItem(
                                movie.getId(),
                                movie.getTitle(),
                                entry.getValue(),
                                priceFor(movie.getId())
                        ));
                    }
                });
        return items;
    }

    public void updateCart(HttpSession session, String movieId, String action) {
        String normalizedMovieId = movieId == null ? null : movieId.trim();
        String normalizedAction = action == null ? null : action.trim().toLowerCase();
        if (normalizedMovieId == null || normalizedMovieId.isEmpty()
                || normalizedAction == null || normalizedAction.isEmpty()) {
            throw new IllegalArgumentException("Missing movieId or action");
        }

        Map<String, Integer> cart = getOrCreateCart(session);
        synchronized (cart) {
            int currentQuantity = cart.getOrDefault(normalizedMovieId, 0);
            switch (normalizedAction) {
                case "add":
                case "increase":
                    cart.put(normalizedMovieId, currentQuantity + 1);
                    break;
                case "decrease":
                    if (currentQuantity <= 1) {
                        cart.remove(normalizedMovieId);
                    } else {
                        cart.put(normalizedMovieId, currentQuantity - 1);
                    }
                    break;
                case "delete":
                    cart.remove(normalizedMovieId);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getOrCreateCart(HttpSession session) {
        Object existingCart = session.getAttribute(CART_SESSION_KEY);
        if (existingCart instanceof Map) {
            return (Map<String, Integer>) existingCart;
        }

        Map<String, Integer> cart = new HashMap<>();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    BigDecimal priceFor(String movieId) {
        int cents = 500 + Math.floorMod(movieId.hashCode(), 1500);
        return BigDecimal.valueOf(cents, 2);
    }
}
