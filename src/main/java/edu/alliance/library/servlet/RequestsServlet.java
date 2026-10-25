package edu.alliance.library.servlet;

import edu.alliance.library.dao.LibraryRequestDao;
import edu.alliance.library.model.Account;
import edu.alliance.library.util.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/requests")
public class RequestsServlet extends HttpServlet {
    private final LibraryRequestDao requests = new LibraryRequestDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("requests", requests.pending());
            request.getRequestDispatcher("/WEB-INF/views/requests.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Book requests could not be loaded " + Database.safeDiagnostic(e));
            request.setAttribute("error", "Book requests are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            long requestId = Long.parseLong(request.getParameter("requestId"));
            String decision = request.getParameter("decision");
            if (!"approve".equals(decision) && !"reject".equals(decision)) {
                throw new IllegalArgumentException("Choose whether to approve or reject the request.");
            }
            Account account = (Account) request.getSession().getAttribute("account");
            requests.process(requestId, account.id(), "approve".equals(decision));
            request.getSession().setAttribute("success", "The request was " + ("approve".equals(decision) ? "approved." : "rejected."));
            response.sendRedirect(request.getContextPath() + "/app/requests");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Book request could not be processed " + Database.safeDiagnostic(e));
            request.setAttribute("error", "The request could not be processed. Check book availability and student borrowing limits.");
            load(request, response);
        }
    }
}
