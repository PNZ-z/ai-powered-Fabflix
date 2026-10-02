package com.fabflix.boot.auth;

import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.stereotype.Component;

@Component
public class JasyptPasswordVerifier implements PasswordVerifier {
    private final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

    @Override
    public boolean matches(String submittedPassword, String encryptedPassword) {
        return submittedPassword != null
                && encryptedPassword != null
                && passwordEncryptor.checkPassword(submittedPassword, encryptedPassword);
    }
}
