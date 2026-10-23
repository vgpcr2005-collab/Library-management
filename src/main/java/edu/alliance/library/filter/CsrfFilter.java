package edu.alliance.library.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class CsrfFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpRequest.setCharacterEncoding("UTF-8");
        String expected = (String) httpRequest.getSession().getAttribute("csrfToken");
        if (expected == null) {
            byte[] token = new byte[32];
            RANDOM.nextBytes(token);
            expected = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
            httpRequest.getSession().setAttribute("csrfToken", expected);
        }
        httpRequest.setAttribute("csrfToken", expected);
        Object flash = httpRequest.getSession().getAttribute("success");
        if (flash != null) {
            httpRequest.setAttribute("success", flash);
            httpRequest.getSession().removeAttribute("success");
        }
        Object error = httpRequest.getSession().getAttribute("error");
        if (error != null) {
            httpRequest.setAttribute("error", error);
            httpRequest.getSession().removeAttribute("error");
        }
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            String supplied = httpRequest.getParameter("csrfToken");
            if (supplied == null || !MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                httpRequest.getSession().setAttribute("error",
                        "Your form session expired, so the request was not submitted. Please review the refreshed form and try again.");
                httpResponse.sendRedirect(httpRequest.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
