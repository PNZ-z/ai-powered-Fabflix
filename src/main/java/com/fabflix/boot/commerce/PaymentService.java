package com.fabflix.boot.commerce;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResult placeOrder(
            String customerEmail,
            Map<String, Integer> cart,
            PaymentRequest request) {
        if (!paymentRepository.paymentCardMatches(request)) {
            return PaymentResult.INVALID_PAYMENT;
        }

        Optional<Integer> customerId = paymentRepository.findCustomerIdByEmail(customerEmail);
        if (customerId.isEmpty()) {
            return PaymentResult.CUSTOMER_NOT_FOUND;
        }

        paymentRepository.insertSales(customerId.get(), cart);
        return PaymentResult.SUCCESS;
    }
}
