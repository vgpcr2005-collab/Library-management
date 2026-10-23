package edu.alliance.library.service;

import edu.alliance.library.dao.BookDao;
import edu.alliance.library.model.Book;
import java.sql.SQLException;
import java.util.List;

public class LibraryService {
    private static final String[] SEARCH_FIELDS = {"title", "author", "ISBN", "category"};
    private final BookDao books = new BookDao();

    public List<Book> searchBooks(String query, String category, String availability) throws SQLException {
        return books.search(query, category, availability);
    }

    public String[] searchFields() { return SEARCH_FIELDS.clone(); }
}
