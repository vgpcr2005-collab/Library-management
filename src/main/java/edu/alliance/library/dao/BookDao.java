package edu.alliance.library.dao;

import edu.alliance.library.model.Book;
import edu.alliance.library.model.LibraryItem;
import edu.alliance.library.model.ReferenceBook;
import edu.alliance.library.util.Database;
import edu.alliance.library.util.InputValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDao {
    public List<Book> search(String query, String category, String availability) throws SQLException {
        String sql = """
                SELECT id,isbn,title,author,category,total_copies,available_copies,reference_only
                FROM books
                WHERE (?='' OR LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(isbn) LIKE ? OR LOWER(category) LIKE ?)
                  AND (?='' OR category=?)
                  AND (?='' OR (?='available' AND available_copies>0) OR (?='unavailable' AND available_copies=0))
                ORDER BY title LIMIT 300
                """;
        String term = InputValidator.normalize(query);
        String selectedCategory = InputValidator.clean(category);
        String selectedAvailability = InputValidator.clean(availability);
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, term);
            for (int i = 2; i <= 5; i++) p.setString(i, "%" + term + "%");
            p.setString(6, selectedCategory);
            p.setString(7, selectedCategory);
            p.setString(8, selectedAvailability);
            p.setString(9, selectedAvailability);
            p.setString(10, selectedAvailability);
            try (ResultSet r = p.executeQuery()) {
                List<Book> books = new ArrayList<>();
                while (r.next()) books.add(map(r));
                return books;
            }
        }
    }

    public List<String> categories() throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT DISTINCT category FROM books ORDER BY category");
             ResultSet r = p.executeQuery()) {
            List<String> values = new ArrayList<>();
            while (r.next()) values.add(r.getString(1));
            return values;
        }
    }

    public void save(Book book) throws SQLException {
        try (Connection c = Database.getConnection()) {
            if (book.getId() == 0) {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO books(isbn,title,author,category,total_copies,available_copies,reference_only) VALUES(?,?,?,?,?,?,?)")) {
                    bindBook(p, book, book.getTotalCopies());
                    if (p.executeUpdate() != 1) throw new SQLException("Book could not be saved");
                }
                return;
            }
            c.setAutoCommit(false);
            try {
                int borrowed;
                try (PreparedStatement p = c.prepareStatement("SELECT total_copies,available_copies FROM books WHERE id=? FOR UPDATE")) {
                    p.setLong(1, book.getId());
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Book not found.");
                        borrowed = r.getInt("total_copies") - r.getInt("available_copies");
                    }
                }
                if (book.getTotalCopies() < borrowed)
                    throw new IllegalArgumentException("Total copies cannot be less than copies currently on loan.");
                String sql = "UPDATE books SET isbn=?,title=?,author=?,category=?,total_copies=?,available_copies=?,reference_only=? WHERE id=?";
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    bindBook(p, book, book.getTotalCopies() - borrowed);
                    p.setLong(8, book.getId());
                    if (p.executeUpdate() != 1) throw new SQLException("Book could not be saved");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void bindBook(PreparedStatement p, Book book, int available) throws SQLException {
        p.setString(1, book.getIsbn());
        p.setString(2, book.getTitle());
        p.setString(3, book.getAuthor());
        p.setString(4, book.getCategory());
        p.setInt(5, book.getTotalCopies());
        p.setInt(6, available);
        p.setBoolean(7, book.isReferenceOnly());
    }

    public void delete(long id) throws SQLException {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM books WHERE id=?")) {
            p.setLong(1, id);
            if (p.executeUpdate() != 1) throw new SQLException("Book was not found");
        }
    }

    public Book find(long id) throws SQLException {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT id,isbn,title,author,category,total_copies,available_copies,reference_only FROM books WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? map(r) : null; }
        }
    }

    private Book map(ResultSet r) throws SQLException {
        long id = r.getLong("id");
        String isbn = r.getString("isbn");
        String title = r.getString("title");
        String author = r.getString("author");
        String category = r.getString("category");
        int total = r.getInt("total_copies");
        int available = r.getInt("available_copies");
        boolean reference = r.getBoolean("reference_only");
        LibraryItem item = reference
                ? new ReferenceBook(id, isbn, title, author, category, total, available)
                : new Book(id, isbn, title, author, category, total, available, false);
        return (Book) item;
    }
}
