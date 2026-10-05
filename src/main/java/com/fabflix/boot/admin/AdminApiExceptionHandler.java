package com.fabflix.boot.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AdminController.class, EmployeeController.class})
public class AdminApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AdminApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AdminResponse> handle(Exception exception) {
        logger.error("Employee API request failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AdminResponse.failure("Unable to complete the employee request."));
    }
}
