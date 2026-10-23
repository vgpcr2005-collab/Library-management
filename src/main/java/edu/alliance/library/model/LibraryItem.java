package edu.alliance.library.model;

public abstract class LibraryItem {
    private final long id;
    private final String title;
    private final String isbn;
    private final String author;

    protected LibraryItem(long id, String title, String isbn, String author) {
        this.id = id;
        this.title = required(title);
        this.isbn = required(isbn);
        this.author = required(author);
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Book details are required");
        return value.trim();
    }

    public abstract String getDetails();
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getIsbn() { return isbn; }
    public String getAuthor() { return author; }
}
