package edu.alliance.library.model;

public class Book extends LibraryItem {
    private final String category;
    private int totalCopies;
    private int availableCopies;
    private final boolean referenceOnly;

    public Book(long id, String isbn, String title, String author, String category,
                int totalCopies, int availableCopies, boolean referenceOnly) {
        super(id, title, isbn, author);
        if (category == null || category.isBlank()) throw new IllegalArgumentException("Category is required");
        validateCopies(totalCopies, availableCopies);
        this.category = category.trim();
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.referenceOnly = referenceOnly;
    }

    private static void validateCopies(int total, int available) {
        if (total < 0 || available < 0 || available > total) throw new IllegalArgumentException("Invalid copy count");
    }

    public void setCopyCounts(int total, int available) {
        validateCopies(total, available);
        this.totalCopies = total;
        this.availableCopies = available;
    }

    @Override
    public String getDetails() {
        return getTitle() + " by " + getAuthor() + " (" + category + ")";
    }

    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public boolean isReferenceOnly() { return referenceOnly; }
}
