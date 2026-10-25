package edu.alliance.library.servlet;

import edu.alliance.library.dao.AccountDao;
import edu.alliance.library.dao.IssueDao;
import edu.alliance.library.dao.LibraryRequestDao;
import edu.alliance.library.dao.StudentDao;
import edu.alliance.library.model.Account;
import edu.alliance.library.util.Database;
import edu.alliance.library.util.InputValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/my-library")
public class MyLibraryServlet extends HttpServlet {
    private final AccountDao accounts = new AccountDao();
    private final StudentDao students = new StudentDao();
    private final IssueDao issues = new IssueDao();
    private final LibraryRequestDao requests = new LibraryRequestDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Account account = (Account) request.getSession().getAttribute("account");
        try {
            request.setAttribute("student", students.findByRecordId(account.studentRecordId()));
            request.setAttribute("loans", issues.history(account.studentRecordId()));
            request.setAttribute("requests", requests.forStudent(account.studentRecordId()));
            request.getRequestDispatcher("/WEB-INF/views/my-library.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Student library information could not be loaded " + Database.safeDiagnostic(e));
            request.setAttribute("error", "Your library information is temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Account account = (Account) request.getSession().getAttribute("account");
        try {
            String action = InputValidator.clean(request.getParameter("action"));
            if ("borrow".equals(action)) {
                requests.requestBook(account.studentRecordId(), Long.parseLong(request.getParameter("bookId")));
                request.getSession().setAttribute("success", "Your book request was sent to library staff.");
                response.sendRedirect(request.getContextPath() + "/app/my-library");
                return;
            }
            if ("renew".equals(action)) {
                requests.requestRenewal(account.studentRecordId(), Long.parseLong(request.getParameter("issueId")));
                request.getSession().setAttribute("success", "Your renewal request was sent to library staff.");
                response.sendRedirect(request.getContextPath() + "/app/my-library");
                return;
            }
            String fullName = InputValidator.clean(request.getParameter("fullName"));
            String email = InputValidator.normalize(request.getParameter("email"));
            String department = InputValidator.clean(request.getParameter("department"));
            String phone = InputValidator.clean(request.getParameter("phone"));
            if (fullName.isBlank() || fullName.length() > 120 || !InputValidator.validEmail(email)
                    || department.isBlank() || department.length() > 100 || phone.isBlank() || phone.length() > 24) {
                throw new IllegalArgumentException("Enter valid values for all profile fields.");
            }
            accounts.updateStudent(account.id(), fullName, email, department, phone);
            request.getSession().setAttribute("account",
                    new Account(account.id(), email, fullName, account.role(), account.studentRecordId()));
            request.getSession().setAttribute("success", "Your profile was updated.");
            response.sendRedirect(request.getContextPath() + "/app/my-library");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Student profile update failed " + Database.safeDiagnostic(e));
            request.setAttribute("error", "Your profile could not be updated. Check whether the email is already registered.");
            load(request, response);
        }
    }
}
