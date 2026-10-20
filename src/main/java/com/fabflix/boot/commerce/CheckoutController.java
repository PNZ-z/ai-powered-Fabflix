package com.fabflix.boot.commerce;

import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    private final CheckoutTokenService checkoutTokenService;
    private final CartService cartService;

    public CheckoutController(CheckoutTokenService checkoutTokenService, CartService cartService) {
        this.checkoutTokenService = checkoutTokenService;
        this.cartService = cartService;
    }

    // Hands the checkout page a fresh token and marks the session pending. It
    // runs inside the cart monitor so it cannot interleave with an in-flight
    // payment, and it never clears the cart.
    @GetMapping("/api/boot/checkout/token")
    public ResponseEntity<Map<String, String>> issueToken(HttpSession session) {
        String token;
        synchronized (cartService.getOrCreateCart(session)) {
            token = checkoutTokenService.issueToken(session);
        }
        return ResponseEntity.ok(Map.of("status", "success", "token", token));
    }

    // The most recent successful order, or 204 when this session has none.
    @GetMapping("/api/boot/confirmation")
    public ResponseEntity<OrderSummary> confirmation(HttpSession session) {
        return checkoutTokenService.recentOrder(session)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
