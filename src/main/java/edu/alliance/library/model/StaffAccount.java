package edu.alliance.library.model;

import java.io.Serializable;

public record StaffAccount(long id, String email, String fullName, String role) implements Serializable {
    public StaffAccount {
        if (id < 1 || email == null || email.isBlank() || fullName == null || fullName.isBlank()
                || !("ADMIN".equals(role) || "LIBRARIAN".equals(role))) {
            throw new IllegalArgumentException("A valid staff account is required.");
        }
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
