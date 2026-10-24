package edu.alliance.library.servlet;

import edu.alliance.library.dao.BookDao;
import edu.alliance.library.dao.IssueDao;
import edu.alliance.library.dao.LibraryOperatorDao;
import edu.alliance.library.dao.StudentDao;
import edu.alliance.library.model.Book;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/issue")
public class IssueServlet extends HttpServlet {
    private final BookDao books = new BookDao();
    private final StudentDao students = new StudentDao();
    private final IssueDao issues = new IssueDao();
    private final LibraryOperatorDao operator = new LibraryOperatorDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("books", books.search("", "", "available").stream().filter(b -> !b.isReferenceOnly()).toList());
            request.setAttribute("students", students.search(""));
            request.getRequestDispatcher("/WEB-INF/views/issue.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Issue form data could not be loaded", e);
            request.setAttribute("error", "Issue form data is temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            long bookId = Long.parseLong(request.getParameter("bookId"));
            long studentRecordId = Long.parseLong(request.getParameter("studentRecordId"));
            issues.issue(bookId, studentRecordId, operator.operatorId());
            request.getSession().setAttribute("success", "Book issued. The due date was set from library settings.");
            response.sendRedirect(request.getContextPath() + "/app/issue");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Issue transaction failed", e);
            request.setAttribute("error", "The issue could not be completed. Please try again.");
            load(request, response);
        }
    }

}
