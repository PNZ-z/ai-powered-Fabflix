package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import com.fabflix.boot.auth.CustomerIdentity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private static final String ORDER_PLACED_MESSAGE = "Order placed successfully.";
    private static final String EMPTY_CART_MESSAGE = "Your shopping cart is empty.";
    private static final String EXPIRED_CHECKOUT_MESSAGE =
            "Your checkout session has expired. Please try again.";
    private static final String INVALID_PAYMENT_MESSAGE = "Invalid payment information.";
    private static final String CUSTOMER_NOT_FOUND_MESSAGE = "Customer not found.";

    private final CartService cartService;
    private final PaymentService paymentService;
    private final CheckoutTokenService checkoutTokenService;

    public PaymentController(
            CartService cartService,
            PaymentService paymentService,
            CheckoutTokenService checkoutTokenService) {
        this.cartService = cartService;
        this.paymentService = paymentService;
        this.checkoutTokenService = checkoutTokenService;
    }

    @PostMapping("/api/boot/payment")
    public ResponseEntity<OperationResponse> pay(
            @ModelAttribute PaymentRequest request,
            HttpSession session) {
        CustomerIdentity customer = (CustomerIdentity) session.getAttribute("user");
        Map<String, Integer> cart = cartService.getOrCreateCart(session);

        // The session cart is the single critical section. The token check, the
        // replay of a completed order, the empty-cart guard, and the order
        // summary are all resolved before the order is placed, so a concurrent
        // or repeated submission can never place the order twice. The summary is
        // assembled (including its database read) before placeOrder so a failure
        // there happens while no sales row exists yet.
        synchronized (cart) {
            if (!checkoutTokenService.matches(session, request.getCheckoutToken())) {
                return ResponseEntity.ok(OperationResponse.failure(EXPIRED_CHECKOUT_MESSAGE));
            }

            Optional<OrderSummary> replayedOrder = checkoutTokenService.completedOrder(session);
            if (replayedOrder.isPresent()) {
                return ResponseEntity.ok(
                        OperationResponse.success(ORDER_PLACED_MESSAGE, replayedOrder.get()));
            }

            if (cart.isEmpty()) {
                return ResponseEntity.ok(OperationResponse.failure(EMPTY_CART_MESSAGE));
            }

            OrderSummary order = summarize(cartService.getCart(session));

            PaymentResult result = paymentService.placeOrder(
                    customer.getUsername(), Map.copyOf(cart), request);
            switch (result) {
                case SUCCESS:
                    checkoutTokenService.completeOrder(session, order);
                    cart.clear();
                    return ResponseEntity.ok(OperationResponse.success(ORDER_PLACED_MESSAGE, order));
                case INVALID_PAYMENT:
                    return ResponseEntity.ok(OperationResponse.failure(INVALID_PAYMENT_MESSAGE));
                case CUSTOMER_NOT_FOUND:
                    return ResponseEntity.ok(OperationResponse.failure(CUSTOMER_NOT_FOUND_MESSAGE));
                default:
                    throw new IllegalStateException("Unsupported payment result: " + result);
            }
        }
    }

    private OrderSummary summarize(List<CartItem> items) {
        List<OrderLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (CartItem item : items) {
            BigDecimal unitPrice = item.getPrice();
            BigDecimal lineTotal = unitPrice
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            lines.add(new OrderLine(
                    item.getMovieId(),
                    item.getMovieTitle(),
                    item.getQuantity(),
                    unitPrice,
                    lineTotal));
        }
        return new OrderSummary(LocalDate.now().toString(), total, lines);
    }
}
