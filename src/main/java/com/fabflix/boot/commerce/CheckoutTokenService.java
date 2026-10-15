package com.fabflix.boot.commerce;

import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

// Owns the session state that makes checkout idempotent: an opaque
// checkoutToken doubles as the idempotency key, checkoutState tracks whether
// the token still awaits an order or already produced one, and checkoutOrder
// caches the successful order so a replay returns the same data instead of
// placing a second order. The token is only meaningful for one session on one
// JVM instance (see the single-instance deployment assumption).
@Service
public class CheckoutTokenService {
    static final String TOKEN_SESSION_KEY = "checkoutToken";
    static final String STATE_SESSION_KEY = "checkoutState";
    static final String ORDER_SESSION_KEY = "checkoutOrder";

    static final String PENDING_STATE = "pending";
    static final String COMPLETED_STATE = "completed";

    public String issueToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(TOKEN_SESSION_KEY, token);
        session.setAttribute(STATE_SESSION_KEY, PENDING_STATE);
        return token;
    }

    public boolean matches(HttpSession session, String token) {
        Object stored = session.getAttribute(TOKEN_SESSION_KEY);
        return token != null && !token.isEmpty() && token.equals(stored);
    }

    // Returns the cached order only after a successful checkout, which is what
    // lets a repeated submission replay the same success response.
    public Optional<OrderSummary> completedOrder(HttpSession session) {
        if (!COMPLETED_STATE.equals(session.getAttribute(STATE_SESSION_KEY))) {
            return Optional.empty();
        }
        return recentOrder(session);
    }

    public void completeOrder(HttpSession session, OrderSummary order) {
        session.setAttribute(ORDER_SESSION_KEY, order);
        session.setAttribute(STATE_SESSION_KEY, COMPLETED_STATE);
    }

    // The most recent successful order, regardless of the current checkout
    // state, so a fresh pending checkout still shows the previous order.
    public Optional<OrderSummary> recentOrder(HttpSession session) {
        Object order = session.getAttribute(ORDER_SESSION_KEY);
        return order instanceof OrderSummary ? Optional.of((OrderSummary) order) : Optional.empty();
    }
}
