package edu.alliance.library.filter;

import edu.alliance.library.dao.AuthDao;
import edu.alliance.library.model.StaffAccount;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class AuthenticationFilter implements Filter {
    private final AuthDao accounts = new AuthDao();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        boolean production = "production".equalsIgnoreCase(System.getenv("APP_ENV"));
        boolean forwardedHttps = "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
        if (production && !httpRequest.isSecure() && !forwardedHttps) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Use the secure HTTPS website URL.");
            return;
        }
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        if (production) httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        if ("/login".equals(path) || "/".equals(path) || "/index.jsp".equals(path)
                || "/register".equals(path) || path.startsWith("/assets/") || path.startsWith("/webjars/")) {
            chain.doFilter(request, response);
            return;
        }
        Object sessionStaff = httpRequest.getSession(false) == null
                ? null : httpRequest.getSession(false).getAttribute("staff");
        if (!(sessionStaff instanceof StaffAccount staff)) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }
        try {
            if (!accounts.isEnabledStaff(staff.id())) {
                httpRequest.getSession(false).invalidate();
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
                return;
            }
        } catch (SQLException e) {
            httpRequest.getServletContext().log("Staff session could not be validated", e);
            httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "The library is temporarily unavailable. Please try again.");
            return;
        }
        if (("/app/settings".equals(path) || "/app/staff".equals(path)) && !staff.isAdmin()) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access is required.");
            return;
        }
        chain.doFilter(request, response);
    }
}
