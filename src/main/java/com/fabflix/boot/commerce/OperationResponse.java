package com.fabflix.boot.commerce;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationResponse {
    private final String status;
    private final String message;

    private OperationResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static OperationResponse success() {
        return new OperationResponse("success", null);
    }

    public static OperationResponse success(String message) {
        return new OperationResponse("success", message);
    }

    public static OperationResponse failure(String message) {
        return new OperationResponse("fail", message);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
