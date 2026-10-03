package com.fabflix.boot.commerce;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/api/boot/cart")
    public ResponseEntity<List<CartItem>> getCart(HttpSession session) {
        return ResponseEntity.ok(cartService.getCart(session));
    }

    @PostMapping("/api/boot/cart")
    public ResponseEntity<OperationResponse> updateCart(
            @RequestParam(value = "movieId", required = false) String movieId,
            @RequestParam(value = "action", required = false) String action,
            HttpSession session) {
        cartService.updateCart(session, movieId, action);
        return ResponseEntity.ok(OperationResponse.success());
    }
}
