package edu.alliance.library.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

public class SecureCookieFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean secure = httpRequest.isSecure()
                || ("production".equalsIgnoreCase(System.getenv("APP_ENV"))
                && "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto")));
        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(httpResponse) {
            @Override
            public void addCookie(Cookie cookie) {
                if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                    cookie.setHttpOnly(true);
                    cookie.setAttribute("SameSite", "Lax");
                    if (secure) cookie.setSecure(true);
                }
                super.addCookie(cookie);
            }
        };
        chain.doFilter(request, wrapped);
    }
}
