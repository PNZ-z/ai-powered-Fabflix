package com.fabflix.boot.auth;

public interface PasswordVerifier {
    boolean matches(String submittedPassword, String encryptedPassword);
}
