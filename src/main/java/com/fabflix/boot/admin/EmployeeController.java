package com.fabflix.boot.admin;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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
}
