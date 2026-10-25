package edu.alliance.library.filter;

import edu.alliance.library.model.Account;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Account account = (Account) httpRequest.getSession().getAttribute("account");
        if (account == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        String path = httpRequest.getServletPath();
        if (account.isStudent() && ("/app/students".equals(path) || "/app/issue".equals(path)
                || "/app/return".equals(path) || "/app/reports".equals(path) || "/app/settings".equals(path)
                || "/app/requests".equals(path)
                || ("POST".equalsIgnoreCase(httpRequest.getMethod()) && "/app/books".equals(path)))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This page is available to library staff only.");
            return;
        }
        if (!account.isStudent() && !account.isStaff()) {
            httpRequest.getSession().invalidate();
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
