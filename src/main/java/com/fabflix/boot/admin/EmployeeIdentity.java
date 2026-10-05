package com.fabflix.boot.admin;

import java.io.Serializable;

public class EmployeeIdentity implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String email;
    private final String fullName;

    public EmployeeIdentity(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
}
