package edu.alliance.library.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import edu.alliance.library.model.Account;

import java.io.IOException;

@WebServlet(urlPatterns = {""})
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        openDashboard(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        openDashboard(request, response);
    }

    private void openDashboard(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Account account = (Account) request.getSession().getAttribute("account");
        response.sendRedirect(request.getContextPath() + (account == null ? "/login" : "/app/dashboard"));
    }
}
