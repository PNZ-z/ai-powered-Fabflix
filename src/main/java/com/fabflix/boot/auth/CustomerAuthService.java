package com.fabflix.boot.auth;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class CustomerAuthService {
    private final CustomerAuthRepository customerAuthRepository;
    private final PasswordVerifier passwordVerifier;

    public CustomerAuthService(
            CustomerAuthRepository customerAuthRepository,
            PasswordVerifier passwordVerifier) {
        this.customerAuthRepository = customerAuthRepository;
        this.passwordVerifier = passwordVerifier;
    }

    public Optional<AuthenticatedCustomer> authenticate(String username, String password) {
        String normalizedUsername = username == null ? null : username.trim();
        if (normalizedUsername == null || normalizedUsername.isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        return customerAuthRepository.findPasswordHashByEmail(normalizedUsername)
                .filter(passwordHash -> passwordVerifier.matches(password, passwordHash))
                .map(passwordHash -> new AuthenticatedCustomer(normalizedUsername));
    }
}
