package edu.alliance.library.model;

public abstract class User {
    private final long id;
    private final String email;
    private final String fullName;
    private final String role;

    protected User(long id, String email, String fullName, String role) {
        this.id = id;
        this.email = require(email, "email");
        this.fullName = require(fullName, "full name");
        this.role = require(role, "role");
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
