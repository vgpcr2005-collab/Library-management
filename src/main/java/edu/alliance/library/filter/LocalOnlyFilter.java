package edu.alliance.library.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.InetAddress;

public class LocalOnlyFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean production = "production".equalsIgnoreCase(System.getenv("APP_ENV"));
        if (!production && !InetAddress.getByName(httpRequest.getRemoteAddr()).isLoopbackAddress()) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "This library application is available on this computer only.");
            return;
        }
        chain.doFilter(request, response);
    }
}
