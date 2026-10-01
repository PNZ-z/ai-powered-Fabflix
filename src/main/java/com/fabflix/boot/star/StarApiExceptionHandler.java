package com.fabflix.boot.star;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StarDetailController.class)
public class StarApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(StarApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(StarNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StarNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Star not found.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedFailure(RuntimeException exception) {
        logger.error("Star detail request failed", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("errorMessage", message));
    }
}
