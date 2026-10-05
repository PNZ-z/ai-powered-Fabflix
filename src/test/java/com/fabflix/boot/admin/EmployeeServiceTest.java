package com.fabflix.boot.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import com.fabflix.boot.auth.PasswordVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {
    private EmployeeRepository repository;
    private PasswordVerifier verifier;
    private EmployeeService service;

    @BeforeEach void setUp() {
        repository = mock(EmployeeRepository.class);
        verifier = mock(PasswordVerifier.class);
        service = new EmployeeService(repository, verifier);
    }

    @Test void authenticatesValidEmployee() {
        when(repository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new EmployeeRecord("hash", "Admin User")));
        when(verifier.matches("secret", "hash")).thenReturn(true);
        EmployeeIdentity identity = service.authenticate(" admin@example.com ", "secret").orElseThrow();
        assertEquals("admin@example.com", identity.getEmail());
        assertEquals("Admin User", identity.getFullName());
    }

    @Test void rejectsMissingCredentialsWithoutDatabaseLookup() {
        assertTrue(service.authenticate(" ", "secret").isEmpty());
        assertTrue(service.authenticate("a@b.com", null).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test void rejectsUnknownEmployeeAndWrongPassword() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertTrue(service.authenticate("missing@example.com", "secret").isEmpty());
        when(repository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new EmployeeRecord("hash", "Admin")));
        when(verifier.matches("wrong", "hash")).thenReturn(false);
        assertTrue(service.authenticate("admin@example.com", "wrong").isEmpty());
    }
}
