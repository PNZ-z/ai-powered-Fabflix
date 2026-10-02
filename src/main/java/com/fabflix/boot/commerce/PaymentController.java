package com.fabflix.boot.commerce;

import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fabflix.boot.auth.CustomerIdentity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private final CartService cartService;
    private final PaymentService paymentService;

    public PaymentController(CartService cartService, PaymentService paymentService) {
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    @PostMapping("/api/boot/payment")
    public ResponseEntity<OperationResponse> pay(
            @ModelAttribute PaymentRequest request,
            HttpSession session) {
        CustomerIdentity customer = (CustomerIdentity) session.getAttribute("user");
        Map<String, Integer> cart = cartService.getOrCreateCart(session);
        if (cart.isEmpty()) {
            return ResponseEntity.ok(OperationResponse.failure("Your shopping cart is empty."));
        }

        PaymentResult result;
        synchronized (cart) {
            result = paymentService.placeOrder(customer.getUsername(), Map.copyOf(cart), request);
            if (result == PaymentResult.SUCCESS) {
                cart.clear();
            }
        }

        switch (result) {
            case SUCCESS:
                return ResponseEntity.ok(OperationResponse.success("Order placed successfully."));
            case INVALID_PAYMENT:
                return ResponseEntity.ok(OperationResponse.failure("Invalid payment information."));
            case CUSTOMER_NOT_FOUND:
                return ResponseEntity.ok(OperationResponse.failure("Customer not found."));
            default:
                throw new IllegalStateException("Unsupported payment result: " + result);
        }
    }
}
