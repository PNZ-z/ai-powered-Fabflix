package com.fabflix.boot.session;

import jakarta.servlet.http.HttpSession;

import com.fabflix.boot.admin.EmployeeIdentity;
import com.fabflix.boot.auth.CustomerIdentity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    @GetMapping("/api/boot/session/customer")
    public ResponseEntity<SessionIdentityResponse> customerSession(HttpSession session) {
        Object identity = session.getAttribute("user");
        if (!(identity instanceof CustomerIdentity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomerIdentity customer = (CustomerIdentity) identity;
        return ResponseEntity.ok(SessionIdentityResponse.customer(customer.getUsername()));
    }

    @GetMapping("/api/boot/dashboard/session")
    public ResponseEntity<SessionIdentityResponse> employeeSession(HttpSession session) {
        Object identity = session.getAttribute("employee");
        if (!(identity instanceof EmployeeIdentity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        EmployeeIdentity employee = (EmployeeIdentity) identity;
        return ResponseEntity.ok(SessionIdentityResponse.employee(
                employee.getEmail(), employee.getFullName()));
    }
}
