package com.fabflix.boot.star;

public class StarNotFoundException extends RuntimeException {
    public StarNotFoundException(String starId) {
        super("Star not found: " + starId);
    }
}
