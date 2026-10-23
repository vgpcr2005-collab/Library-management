package edu.alliance.library.model;

public final class Librarian extends User {
    public Librarian(long id, String email, String fullName, String role) {
        super(id, email, fullName, role);
        if (!"LIBRARIAN".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("A library staff role is required");
        }
    }
}
