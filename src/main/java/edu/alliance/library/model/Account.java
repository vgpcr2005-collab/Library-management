package edu.alliance.library.model;

import java.io.Serializable;

public record Account(long id, String email, String fullName, String role, long studentRecordId)
        implements Serializable {
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }

    public boolean isStaff() {
        return "LIBRARIAN".equals(role) || "ADMIN".equals(role);
    }
}
