package edu.alliance.library.model;

public final class ReferenceBook extends Book {
    public ReferenceBook(long id, String isbn, String title, String author, String category,
                         int totalCopies, int availableCopies) {
        super(id, isbn, title, author, category, totalCopies, availableCopies, true);
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " — library use only";
    }
}
