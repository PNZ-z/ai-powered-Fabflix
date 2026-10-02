package com.fabflix.boot.auth;

public interface RecaptchaVerifier {
    boolean verify(String responseToken);
}
