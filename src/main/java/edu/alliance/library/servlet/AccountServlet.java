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
import java.util.Arrays;

@WebServlet("/app/account")
public class AccountServlet extends HttpServlet {
    private final AuthDao auth = new AuthDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StaffAccount staff = (StaffAccount) request.getSession().getAttribute("staff");
        char[] currentPassword = value(request, "currentPassword");
        char[] newPassword = value(request, "newPassword");
        char[] confirmPassword = value(request, "confirmPassword");
        try {
            if (!Arrays.equals(newPassword, confirmPassword)) {
                throw new IllegalArgumentException("The new passwords do not match.");
            }
            auth.changePassword(staff.id(), currentPassword, newPassword);
            request.getSession().setAttribute("success", "Your password was changed.");
            response.sendRedirect(request.getContextPath() + "/app/account");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Staff password update failed", e);
            request.setAttribute("error", "Your password could not be changed. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/account.jsp").forward(request, response);
        } finally {
            Arrays.fill(currentPassword, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirmPassword, '\0');
        }
    }

    private char[] value(HttpServletRequest request, String parameter) {
        String value = request.getParameter(parameter);
        return value == null ? new char[0] : value.toCharArray();
    }
}
