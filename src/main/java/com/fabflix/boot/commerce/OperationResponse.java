package com.fabflix.boot.commerce;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationResponse {
    private final String status;
    private final String message;
    private final OrderSummary order;

    private OperationResponse(String status, String message, OrderSummary order) {
        this.status = status;
        this.message = message;
        this.order = order;
    }

    public static OperationResponse success() {
        return new OperationResponse("success", null, null);
    }

    public static OperationResponse success(String message, OrderSummary order) {
        return new OperationResponse("success", message, order);
    }

    public static OperationResponse failure(String message) {
        return new OperationResponse("fail", message, null);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public OrderSummary getOrder() {
        return order;
    }
}
