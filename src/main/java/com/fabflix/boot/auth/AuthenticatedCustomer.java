package com.fabflix.boot.auth;

import java.io.Serializable;

public class AuthenticatedCustomer implements CustomerIdentity, Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;

    public AuthenticatedCustomer(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
