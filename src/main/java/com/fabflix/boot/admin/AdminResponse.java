package com.fabflix.boot.admin;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminResponse {
    private final String status;
    private final String message;

    private AdminResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static AdminResponse success(String message) { return new AdminResponse("success", message); }
    public static AdminResponse failure(String message) { return new AdminResponse("fail", message); }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}
