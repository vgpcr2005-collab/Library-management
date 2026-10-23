package edu.alliance.library.servlet;

import edu.alliance.library.dao.AuthDao;
import edu.alliance.library.model.StaffAccount;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/staff")
public class StaffServlet extends HttpServlet {
    private final AuthDao accounts = new AuthDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("staffAccounts", accounts.listStaff());
            request.getRequestDispatcher("/WEB-INF/views/staff.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Staff accounts could not be loaded", e);
            request.setAttribute("error", "Staff accounts are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StaffAccount current = (StaffAccount) request.getSession().getAttribute("staff");
        try {
            String action = request.getParameter("action");
            if ("create".equals(action)) {
                char[] password = request.getParameter("password") == null
                        ? new char[0] : request.getParameter("password").toCharArray();
                try {
                    accounts.createLibrarian(request.getParameter("email"), request.getParameter("fullName"), password);
                } finally {
                    java.util.Arrays.fill(password, '\0');
                }
                request.getSession().setAttribute("success", "Librarian account created. Ask them to sign in and change their password.");
            } else if ("toggle".equals(action)) {
                long accountId = Long.parseLong(request.getParameter("accountId"));
                String enabledValue = request.getParameter("enabled");
                if (!"true".equals(enabledValue) && !"false".equals(enabledValue)) {
                    throw new IllegalArgumentException("Choose a valid staff account status.");
                }
                boolean enabled = Boolean.parseBoolean(enabledValue);
                if (accountId == current.id()) throw new IllegalArgumentException("You cannot disable your own account.");
                accounts.setEnabled(accountId, enabled);
                request.getSession().setAttribute("success", enabled ? "Librarian account enabled." : "Librarian account disabled.");
            } else {
                throw new IllegalArgumentException("Choose a valid staff account action.");
            }
            response.sendRedirect(request.getContextPath() + "/app/staff");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Staff account change failed", e);
            request.setAttribute("error", "The staff account could not be updated.");
            load(request, response);
        }
    }
}
