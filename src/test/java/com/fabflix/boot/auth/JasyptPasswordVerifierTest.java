package com.fabflix.boot.auth;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JasyptPasswordVerifierTest {
    @Test
    void verifiesEncryptedPasswordWithoutAcceptingWrongOrMissingValues() {
        String encrypted = new StrongPasswordEncryptor().encryptPassword("correct-password");
        JasyptPasswordVerifier verifier = new JasyptPasswordVerifier();

        assertTrue(verifier.matches("correct-password", encrypted));
        assertFalse(verifier.matches("wrong-password", encrypted));
        assertFalse(verifier.matches(null, encrypted));
        assertFalse(verifier.matches("correct-password", null));
    }
}
