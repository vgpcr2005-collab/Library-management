package edu.alliance.library.filter;

import edu.alliance.library.util.Database;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityHeadersFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean production = Database.isProductionRuntime();
        boolean forwardedHttps = "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
        if (production && !httpRequest.isSecure() && !forwardedHttps) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Use the secure HTTPS website URL.");
            return;
        }
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        if (production) httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        chain.doFilter(request, response);
    }
}
