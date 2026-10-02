package com.fabflix.boot.auth;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CustomerAuthController.class)
public class AuthApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthApiExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedFailure(RuntimeException exception) {
        logger.error("Customer authentication request failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("errorMessage", "Authentication service unavailable."));
    }
}
