package com.fabflix.boot.commerce;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CartController.class, PaymentController.class})
public class CommerceApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommerceApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("errorMessage", exception.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedFailure(RuntimeException exception) {
        logger.error("Commerce request failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("errorMessage", "Unable to process commerce request."));
    }
}
