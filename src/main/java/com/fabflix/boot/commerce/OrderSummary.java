package com.fabflix.boot.commerce;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

// The order data shown on the confirmation page. It is cached in the session
// (checkoutOrder) so the confirmation page survives a refresh, so it must stay
// Serializable. The same shape is reused by the payment response and by
// GET /api/boot/confirmation.
public class OrderSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String orderDate;
    private final BigDecimal totalAmount;
    private final List<OrderLine> lines;

    public OrderSummary(String orderDate, BigDecimal totalAmount, List<OrderLine> lines) {
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.lines = lines;
    }

    @JsonProperty("order_date")
    public String getOrderDate() {
        return orderDate;
    }

    @JsonProperty("total_amount")
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
