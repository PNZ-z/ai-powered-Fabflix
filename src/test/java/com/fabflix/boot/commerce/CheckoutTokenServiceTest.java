package com.fabflix.boot.commerce;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutTokenServiceTest {
    private final CheckoutTokenService service = new CheckoutTokenService();

    @Test
    void issuesNonBlankTokenAndMarksSessionPending() {
        MockHttpSession session = new MockHttpSession();

        String token = service.issueToken(session);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(token, session.getAttribute(CheckoutTokenService.TOKEN_SESSION_KEY));
        assertEquals("pending", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
        assertFalse(service.completedOrder(session).isPresent());
    }

    @Test
    void rejectsMissingOrMismatchedTokens() {
        MockHttpSession session = new MockHttpSession();
        String token = service.issueToken(session);

        assertFalse(service.matches(session, null));
        assertFalse(service.matches(session, ""));
        assertFalse(service.matches(session, "another-token"));
        assertTrue(service.matches(session, token));
    }

    @Test
    void cachesCompletedOrderAndReplaysIt() {
        MockHttpSession session = new MockHttpSession();
        service.issueToken(session);
        OrderSummary order = sampleOrder();

        service.completeOrder(session, order);

        assertEquals("completed", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
        assertSame(order, service.completedOrder(session).orElseThrow());
        assertSame(order, service.recentOrder(session).orElseThrow());
    }

    @Test
    void reportsNoOrderBeforeCompletion() {
        MockHttpSession session = new MockHttpSession();
        service.issueToken(session);

        assertFalse(service.recentOrder(session).isPresent());
        assertFalse(service.completedOrder(session).isPresent());
    }

    @Test
    void reissuingTokenAfterCompletionReturnsToPending() {
        MockHttpSession session = new MockHttpSession();
        service.issueToken(session);
        service.completeOrder(session, sampleOrder());

        service.issueToken(session);

        assertEquals("pending", session.getAttribute(CheckoutTokenService.STATE_SESSION_KEY));
        assertFalse(service.completedOrder(session).isPresent());
    }

    private OrderSummary sampleOrder() {
        return new OrderSummary("2026-10-14", new BigDecimal("15.00"), List.of(
                new OrderLine("tt0001", "Example", 2, new BigDecimal("7.50"), new BigDecimal("15.00"))));
    }
}
