package com.fabflix.boot.auth;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerAuthController {
    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthController.class);
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final CustomerAuthService customerAuthService;
    private final RecaptchaVerifier recaptchaVerifier;

    public CustomerAuthController(
            CustomerAuthService customerAuthService,
            RecaptchaVerifier recaptchaVerifier) {
        this.customerAuthService = customerAuthService;
        this.recaptchaVerifier = recaptchaVerifier;
    }

    @PostMapping("/api/boot/login")
    public ResponseEntity<LoginResponse> login(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "g-recaptcha-response", required = false) String recaptchaResponse,
            HttpServletRequest request,
            HttpSession session) {
        if (!recaptchaVerifier.verify(recaptchaResponse)) {
            logger.warn("Customer login rejected because reCAPTCHA verification failed");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(LoginResponse.failure("Recaptcha Not Passed"));
        }

        Optional<AuthenticatedCustomer> customer = customerAuthService.authenticate(username, password);
        if (customer.isEmpty()) {
            logger.warn("Customer login rejected because credentials were invalid");
            return ResponseEntity.ok(LoginResponse.failure(INVALID_CREDENTIALS));
        }

        request.changeSessionId();
        session.setAttribute("user", customer.get());
        logger.info("Customer login completed successfully");
        return ResponseEntity.ok(LoginResponse.success());
    }

    @GetMapping("/api/boot/logout")
    public void logout(HttpSession session, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        session.invalidate();
        logger.info("Customer session invalidated");
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            response.sendRedirect("/login.html");
        }
    }
}
