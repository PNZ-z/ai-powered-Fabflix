package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fabflix.boot.auth.AuthenticatedCustomer;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Deterministic two-thread test of the checkout critical section. It calls the
// controller directly with a shared session and cart (never through MockMvc,
// whose MockHttpSession attributes are not concurrency safe). The first
// placeOrder call is pinned with a latch, and the test waits until the second
// submission is actually BLOCKED on the cart monitor before releasing the
// first, so the two submissions genuinely overlap instead of racing on timing.
class PaymentControllerConcurrencyTest {
    private static final long TIMEOUT_SECONDS = 5;

    @Test
    void placesOrderExactlyOnceForConcurrentSubmissions() throws Exception {
        Map<String, Integer> cart = new HashMap<>();
        cart.put("tt0001", 1);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new AuthenticatedCustomer("viewer@example.com"));
        session.setAttribute("cart", cart);

        CheckoutTokenService tokenService = new CheckoutTokenService();
        String token = tokenService.issueToken(session);

        CartService cartService = mock(CartService.class);
        when(cartService.getOrCreateCart(session)).thenReturn(cart);
        when(cartService.getCart(session)).thenReturn(List.of(
                new CartItem("tt0001", "Example", 1, new BigDecimal("7.50"))));

        PaymentService paymentService = mock(PaymentService.class);
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch firstOrderEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstOrder = new CountDownLatch(1);
        when(paymentService.placeOrder(any(), any(), any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                firstOrderEntered.countDown();
                releaseFirstOrder.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            return PaymentResult.SUCCESS;
        });

        PaymentController controller = new PaymentController(cartService, paymentService, tokenService);
        PaymentRequest request = new PaymentRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setCreditCardNumber("1234");
        request.setExpirationDate("2030-01-01");
        request.setCheckoutToken(token);

        AtomicReference<ResponseEntity<OperationResponse>> firstResult = new AtomicReference<>();
        AtomicReference<ResponseEntity<OperationResponse>> secondResult = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread firstThread = new Thread(() -> {
            try {
                firstResult.set(controller.pay(request, session));
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        }, "checkout-first");
        Thread secondThread = new Thread(() -> {
            try {
                secondResult.set(controller.pay(request, session));
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        }, "checkout-second");

        firstThread.start();
        try {
            assertTrue(firstOrderEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the first submission should reach placeOrder");

            secondThread.start();
            awaitBlockedOnCartMonitor(secondThread);
        } finally {
            releaseFirstOrder.countDown();
        }

        firstThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        secondThread.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

        assertFalse(firstThread.isAlive(), "the first submission did not finish");
        assertFalse(secondThread.isAlive(), "the second submission did not finish");
        if (failure.get() != null) {
            throw new AssertionError("a concurrent submission failed", failure.get());
        }

        verify(paymentService, times(1)).placeOrder(any(), any(), any());
        assertEquals("success", firstResult.get().getBody().getStatus());
        assertEquals("success", secondResult.get().getBody().getStatus());
        assertSame(
                firstResult.get().getBody().getOrder(),
                secondResult.get().getBody().getOrder(),
                "the second submission must replay the cached order instance");
        assertTrue(cart.isEmpty());
    }

    // The only monitor PaymentController.pay acquires is the session cart, so a
    // BLOCKED thread is waiting for that cart; poll until it is observed.
    private void awaitBlockedOnCartMonitor(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (thread.getState() == Thread.State.BLOCKED) {
                return;
            }
            if (!thread.isAlive()) {
                throw new AssertionError(
                        "the second submission finished before it could block on the cart monitor");
            }
            Thread.sleep(5);
        }
        throw new AssertionError("the second submission did not block on the cart monitor within "
                + TIMEOUT_SECONDS + "s (state=" + thread.getState() + ")");
    }
}
