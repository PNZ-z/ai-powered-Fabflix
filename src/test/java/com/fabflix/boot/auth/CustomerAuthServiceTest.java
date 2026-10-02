package com.fabflix.boot.auth;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAuthServiceTest {
    @Test
    void authenticatesAndNormalizesValidCredentials() {
        CustomerAuthRepository repository = mock(CustomerAuthRepository.class);
        PasswordVerifier passwordVerifier = mock(PasswordVerifier.class);
        when(repository.findPasswordHashByEmail("viewer@example.com"))
                .thenReturn(Optional.of("encrypted-password"));
        when(passwordVerifier.matches("correct-password", "encrypted-password")).thenReturn(true);

        AuthenticatedCustomer customer = new CustomerAuthService(repository, passwordVerifier)
                .authenticate("  viewer@example.com  ", "correct-password")
                .orElseThrow();

        assertEquals("viewer@example.com", customer.getUsername());
    }

    @Test
    void rejectsMissingCredentialsWithoutQueryingDatabase() {
        CustomerAuthRepository repository = mock(CustomerAuthRepository.class);
        CustomerAuthService service = new CustomerAuthService(repository, mock(PasswordVerifier.class));

        assertTrue(service.authenticate(null, "password").isEmpty());
        assertTrue(service.authenticate("viewer@example.com", "").isEmpty());
        verify(repository, never()).findPasswordHashByEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownCustomerOrIncorrectPassword() {
        CustomerAuthRepository repository = mock(CustomerAuthRepository.class);
        PasswordVerifier passwordVerifier = mock(PasswordVerifier.class);
        when(repository.findPasswordHashByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(repository.findPasswordHashByEmail("viewer@example.com"))
                .thenReturn(Optional.of("encrypted-password"));
        when(passwordVerifier.matches("wrong-password", "encrypted-password")).thenReturn(false);
        CustomerAuthService service = new CustomerAuthService(repository, passwordVerifier);

        assertTrue(service.authenticate("missing@example.com", "password").isEmpty());
        assertTrue(service.authenticate("viewer@example.com", "wrong-password").isEmpty());
    }
}
