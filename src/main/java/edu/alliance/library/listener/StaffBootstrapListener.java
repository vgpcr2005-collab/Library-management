package edu.alliance.library.listener;

import edu.alliance.library.dao.AuthDao;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.SQLException;

@WebListener
public class StaffBootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String email = System.getenv("LIBRARY_BOOTSTRAP_ADMIN_EMAIL");
        String password = System.getenv("LIBRARY_BOOTSTRAP_ADMIN_PASSWORD");
        if ((email == null || email.isBlank()) && (password == null || password.isBlank())) {
            event.getServletContext().log("No bootstrap administrator configured; staff login is unavailable until an administrator is provisioned.");
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap administrator environment variables must be set.");
        }
        char[] secret = password.toCharArray();
        try {
            new AuthDao().bootstrapAdmin(email, secret);
            event.getServletContext().log("Bootstrap administrator is ready.");
        } catch (SQLException | RuntimeException e) {
            throw new IllegalStateException("The bootstrap administrator could not be provisioned.", e);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
    }
}
