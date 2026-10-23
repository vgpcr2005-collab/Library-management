package edu.alliance.library.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/app/about", "/app/concepts", "/app/architecture"})
public class InfoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String page = path.substring(path.lastIndexOf('/') + 1);
        request.getRequestDispatcher("/WEB-INF/views/" + page + ".jsp").forward(request, response);
    }
}
