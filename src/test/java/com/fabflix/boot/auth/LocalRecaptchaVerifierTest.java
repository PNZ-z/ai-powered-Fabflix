package com.fabflix.boot.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocalRecaptchaVerifierTest {
    private final LocalRecaptchaVerifier verifier = new LocalRecaptchaVerifier();

    @Test
    void acceptsLoginWithoutARecaptchaTokenForLocalDevelopment() {
        assertTrue(verifier.verify(null));
        assertTrue(verifier.verify(""));
    }
}
