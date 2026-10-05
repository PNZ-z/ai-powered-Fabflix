package com.fabflix.boot.admin;

class EmployeeRecord {
    private final String passwordHash;
    private final String fullName;

    EmployeeRecord(String passwordHash, String fullName) {
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    String getPasswordHash() { return passwordHash; }
    String getFullName() { return fullName; }
}
