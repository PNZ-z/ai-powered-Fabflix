package com.fabflix.boot.commerce;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    @Test
    void rejectsInvalidPaymentBeforeLookingUpCustomer() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentRequest request = new PaymentRequest();
        when(repository.paymentCardMatches(request)).thenReturn(false);

        PaymentResult result = new PaymentService(repository)
                .placeOrder("viewer@example.com", Map.of("tt0001", 1), request);

        assertEquals(PaymentResult.INVALID_PAYMENT, result);
        verify(repository, never()).findCustomerIdByEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsMissingCustomerWithoutWritingSales() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentRequest request = new PaymentRequest();
        when(repository.paymentCardMatches(request)).thenReturn(true);
        when(repository.findCustomerIdByEmail("missing@example.com")).thenReturn(Optional.empty());

        PaymentResult result = new PaymentService(repository)
                .placeOrder("missing@example.com", Map.of("tt0001", 1), request);

        assertEquals(PaymentResult.CUSTOMER_NOT_FOUND, result);
        verify(repository, never()).insertSales(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void writesEverySaleForValidOrder() {
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentRequest request = new PaymentRequest();
        Map<String, Integer> cart = Map.of("tt0001", 2);
        when(repository.paymentCardMatches(request)).thenReturn(true);
        when(repository.findCustomerIdByEmail("viewer@example.com")).thenReturn(Optional.of(42));

        PaymentResult result = new PaymentService(repository)
                .placeOrder("viewer@example.com", cart, request);

        assertEquals(PaymentResult.SUCCESS, result);
        verify(repository).insertSales(42, cart);
    }
}
