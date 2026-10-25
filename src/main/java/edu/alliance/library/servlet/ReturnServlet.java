package edu.alliance.library.servlet;

import edu.alliance.library.dao.IssueDao;
import edu.alliance.library.model.Account;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/return")
public class ReturnServlet extends HttpServlet {
    private final IssueDao issues = new IssueDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("issues", issues.activeIssues(request.getParameter("q")));
            request.getRequestDispatcher("/WEB-INF/views/returns.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Active issues could not be loaded " + edu.alliance.library.util.Database.safeDiagnostic(e));
            request.setAttribute("error", "Active loans are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Account account = (Account) request.getSession().getAttribute("account");
            var fine = issues.returnBook(Long.parseLong(request.getParameter("issueId")), account.id());
            request.getSession().setAttribute("success", "Return recorded. Fine calculated: " + fine.toPlainString());
            response.sendRedirect(request.getContextPath() + "/app/return");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Return transaction failed " + edu.alliance.library.util.Database.safeDiagnostic(e));
            request.setAttribute("error", "The return could not be completed. Please try again.");
            load(request, response);
        }
    }

}
