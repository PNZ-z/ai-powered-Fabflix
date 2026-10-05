package com.fabflix.boot.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FullTextQueryBuilder {
    public String build(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        List<String> tokens = new ArrayList<>();
        for (String rawToken : input.trim().split("\\s+")) {
            String token = rawToken.replaceAll("[^a-zA-Z0-9]", "");
            if (!token.isEmpty()) {
                tokens.add("+" + token + "*");
            }
        }
        return String.join(" ", tokens);
    }
}
