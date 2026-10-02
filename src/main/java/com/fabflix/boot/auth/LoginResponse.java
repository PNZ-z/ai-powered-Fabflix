package com.fabflix.boot.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private final String status;
    private final String message;

    private LoginResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static LoginResponse success() {
        return new LoginResponse("success", null);
    }

    public static LoginResponse failure(String message) {
        return new LoginResponse("fail", message);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
