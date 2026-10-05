package com.fabflix.boot.admin;

import java.util.Optional;
import com.fabflix.boot.auth.PasswordVerifier;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {
    private final EmployeeRepository repository;
    private final PasswordVerifier passwordVerifier;

    public EmployeeService(EmployeeRepository repository, PasswordVerifier passwordVerifier) {
        this.repository = repository;
        this.passwordVerifier = passwordVerifier;
    }

    public Optional<EmployeeIdentity> authenticate(String email, String password) {
        String normalized = email == null ? null : email.trim();
        if (normalized == null || normalized.isEmpty() || password == null || password.isEmpty()) return Optional.empty();
        return repository.findByEmail(normalized)
                .filter(record -> passwordVerifier.matches(password, record.getPasswordHash()))
                .map(record -> new EmployeeIdentity(normalized, record.getFullName()));
    }
}
