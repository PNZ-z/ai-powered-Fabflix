package com.fabflix.boot.auth;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleRecaptchaVerifier implements RecaptchaVerifier {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate;
    private final String secretKey;

    public GoogleRecaptchaVerifier(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${fabflix.recaptcha.secret-key:}") String secretKey) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.secretKey = secretKey;
    }

    @Override
    public boolean verify(String responseToken) {
        if (responseToken == null || responseToken.isBlank() || secretKey.isBlank()) {
            return false;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", responseToken);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                VERIFY_URL,
                org.springframework.http.RequestEntity.post(VERIFY_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form),
                Map.class
        );
        return response != null && Boolean.TRUE.equals(response.get("success"));
    }
}
