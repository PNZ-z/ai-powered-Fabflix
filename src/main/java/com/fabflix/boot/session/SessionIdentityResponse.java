package com.fabflix.boot.session;

public class SessionIdentityResponse {
    private final boolean authenticated;
    private final String role;
    private final String username;
    private final String displayName;

    private SessionIdentityResponse(String role, String username, String displayName) {
        this.authenticated = true;
        this.role = role;
        this.username = username;
        this.displayName = displayName;
    }

    public static SessionIdentityResponse customer(String username) {
        return new SessionIdentityResponse("customer", username, null);
    }

    public static SessionIdentityResponse employee(String username, String displayName) {
        return new SessionIdentityResponse("employee", username, displayName);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }
}
