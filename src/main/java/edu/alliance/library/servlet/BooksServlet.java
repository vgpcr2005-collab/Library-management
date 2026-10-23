package edu.alliance.library.servlet;

import edu.alliance.library.dao.BookDao;
import edu.alliance.library.model.Book;
import edu.alliance.library.service.LibraryService;
import edu.alliance.library.util.InputValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/books")
public class BooksServlet extends HttpServlet {
    private final BookDao books = new BookDao();
    private final LibraryService library = new LibraryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("books", library.searchBooks(request.getParameter("q"), request.getParameter("category"),
                    request.getParameter("availability")));
            request.setAttribute("categories", books.categories());
            request.setAttribute("searchFields", library.searchFields());
            request.getRequestDispatcher("/WEB-INF/views/books.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Book catalog could not be loaded", e);
            request.setAttribute("error", "The book catalog is temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String action = request.getParameter("action");
            if ("delete".equals(action)) {
                books.delete(Long.parseLong(request.getParameter("id")));
                request.getSession().setAttribute("success", "Book removed from the catalog.");
            } else {
                long id = request.getParameter("id") == null || request.getParameter("id").isBlank()
                        ? 0 : Long.parseLong(request.getParameter("id"));
                String isbn = InputValidator.clean(request.getParameter("isbn")).replaceAll("[- ]", "");
                if (!InputValidator.validIsbn(isbn)) throw new IllegalArgumentException("Enter a valid 10- or 13-digit ISBN.");
                int total = Integer.parseInt(request.getParameter("totalCopies"));
                Book book = new Book(id, isbn, InputValidator.clean(request.getParameter("title")),
                        InputValidator.clean(request.getParameter("author")), InputValidator.clean(request.getParameter("category")),
                        total, total, request.getParameter("referenceOnly") != null);
                books.save(book);
                request.getSession().setAttribute("success", id == 0 ? "Book added successfully." : "Book details updated.");
            }
            response.sendRedirect(request.getContextPath() + "/app/books");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Book update failed", e);
            request.setAttribute("error", "The book could not be saved. Check for a duplicate ISBN and try again.");
            load(request, response);
        }
    }
}
