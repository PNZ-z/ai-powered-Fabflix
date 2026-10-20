package com.fabflix.boot.admin;

import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) { this.employeeService = employeeService; }

    @PostMapping("/api/boot/employee-login")
    public ResponseEntity<AdminResponse> login(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest request, HttpSession session) {
        Optional<EmployeeIdentity> employee = employeeService.authenticate(username, password);
        if (employee.isEmpty()) {
            logger.warn("Employee login rejected because credentials were invalid");
            return ResponseEntity.ok(AdminResponse.failure("Invalid employee email or password."));
        }
        request.changeSessionId();
        session.setAttribute("employee", employee.get());
        logger.info("Employee login completed successfully");
        return ResponseEntity.ok(AdminResponse.success("Employee login successful."));
    }

    // Employee sign-out only drops the employee identity: the same session may
    // also hold a customer ("user") identity that must stay signed in, so the
    // session is not invalidated.
    @GetMapping("/api/boot/dashboard/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.removeAttribute("employee");
        logger.info("Employee logout completed successfully");
        return ResponseEntity.noContent().build();
    }
}
