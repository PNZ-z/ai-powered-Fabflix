package com.fabflix.boot.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalRecaptchaVerifier implements RecaptchaVerifier {
    @Override
    public boolean verify(String responseToken) {
        return true;
    }
}
