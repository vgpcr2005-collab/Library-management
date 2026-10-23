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
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final ConcurrentHashMap<String, AttemptWindow> ATTEMPTS = new ConcurrentHashMap<>();
    private final AuthDao auth = new AuthDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getSession().getAttribute("staff") instanceof StaffAccount) {
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
            return;
        }
        try {
            request.setAttribute("staffConfigured", auth.hasEnabledStaff());
        } catch (SQLException e) {
            getServletContext().log("Could not check whether staff sign-in is configured", e);
            request.setAttribute("error", "Sign-in is temporarily unavailable. Please try again.");
        }
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String rawPassword = request.getParameter("password");
        String throttleKey = request.getRemoteAddr() + "|" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
        long now = System.currentTimeMillis();
        if (!allowAttempt(throttleKey, now)) {
            request.setAttribute("error", "Too many sign-in attempts. Wait 15 minutes and try again.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }
        if (email == null || rawPassword == null || rawPassword.length() > 1024) {
            recordFailure(throttleKey, now);
            request.setAttribute("error", "Email or password is incorrect.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        char[] password = rawPassword.toCharArray();
        rawPassword = null;
        try {
            StaffAccount account = auth.authenticate(email, password);
            if (account == null) {
                recordFailure(throttleKey, now);
                request.setAttribute("error", "Email or password is incorrect.");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }
            ATTEMPTS.remove(throttleKey);
            request.changeSessionId();
            request.getSession().setAttribute("staff", account);
            response.sendRedirect(request.getContextPath() + "/app/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Staff authentication failed due to a database error", e);
            request.setAttribute("error", "Sign-in is temporarily unavailable. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static synchronized boolean allowAttempt(String key, long now) {
        prune(now);
        AttemptWindow window = ATTEMPTS.get(key);
        return window == null || now - window.startedAt >= WINDOW_MILLIS || window.failures < MAX_ATTEMPTS;
    }

    private static synchronized void recordFailure(String key, long now) {
        prune(now);
        AttemptWindow current = ATTEMPTS.get(key);
        if (current == null || now - current.startedAt >= WINDOW_MILLIS) {
            ATTEMPTS.put(key, new AttemptWindow(now, 1));
        } else {
            ATTEMPTS.put(key, new AttemptWindow(current.startedAt, current.failures + 1));
        }
    }

    private static void prune(long now) {
        ATTEMPTS.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS);
        if (ATTEMPTS.size() > 10_000) ATTEMPTS.clear();
    }

    private record AttemptWindow(long startedAt, int failures) { }
}
