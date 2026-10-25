package edu.alliance.library.servlet;

import edu.alliance.library.dao.AccountDao;
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
import java.util.Arrays;

@WebServlet(urlPatterns = {"/login", "/register", "/logout"})
public class AuthenticationServlet extends HttpServlet {
    private final AccountDao accounts = new AccountDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Account account = (Account) request.getSession().getAttribute("account");
        if (account != null) {
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            return;
        }
        request.setAttribute("csrfToken", request.getSession().getAttribute("csrfToken"));
        String view = "/logout".equals(request.getServletPath()) ? "/login" : request.getServletPath();
        request.getRequestDispatcher(view + ".jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = request.getServletPath();
        if ("/logout".equals(path)) {
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        char[] password = request.getParameter("password") == null ? new char[0]
                : request.getParameter("password").toCharArray();
        try {
            if ("/register".equals(path)) {
                register(request, response, password);
            } else {
                login(request, response, password);
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response, char[] password)
            throws IOException, ServletException {
        try {
            String email = InputValidator.normalize(request.getParameter("email"));
            Account account = accounts.authenticate(email, password);
            if (account == null) {
                request.setAttribute("csrfToken", request.getSession().getAttribute("csrfToken"));
                request.setAttribute("error", "Email or password is incorrect.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }
            request.getSession().invalidate();
            request.getSession(true).setAttribute("account", account);
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Sign-in failed " + Database.safeDiagnostic(e));
            request.setAttribute("csrfToken", request.getSession().getAttribute("csrfToken"));
            request.setAttribute("error", "Sign-in is temporarily unavailable. Please try again.");
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response, char[] password)
            throws IOException, ServletException {
        try {
            String fullName = InputValidator.clean(request.getParameter("fullName"));
            String email = InputValidator.normalize(request.getParameter("email"));
            String studentId = InputValidator.clean(request.getParameter("studentId"));
            String department = InputValidator.clean(request.getParameter("department"));
            String phone = InputValidator.clean(request.getParameter("phone"));
            if (fullName.isBlank() || fullName.length() > 120 || studentId.isBlank() || studentId.length() > 40
                    || department.isBlank() || department.length() > 100 || phone.isBlank() || phone.length() > 24
                    || !InputValidator.validEmail(email) || password.length < 12 || password.length > 128) {
                throw new IllegalArgumentException("Enter valid profile details and a password between 12 and 128 characters.");
            }
            Account account = accounts.registerStudent(email, fullName, studentId, department, phone, password);
            request.getSession().invalidate();
            request.getSession(true).setAttribute("account", account);
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
        } catch (IllegalArgumentException e) {
            request.setAttribute("csrfToken", request.getSession().getAttribute("csrfToken"));
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Student registration failed " + Database.safeDiagnostic(e));
            request.setAttribute("csrfToken", request.getSession().getAttribute("csrfToken"));
            request.setAttribute("error", "Your account could not be created. Check whether the email or student ID is already used.");
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }
}
