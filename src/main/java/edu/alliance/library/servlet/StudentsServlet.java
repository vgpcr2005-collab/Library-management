package edu.alliance.library.servlet;

import edu.alliance.library.dao.StudentDao;
import edu.alliance.library.util.InputValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/students")
public class StudentsServlet extends HttpServlet {
    private final StudentDao students = new StudentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("students", students.search(request.getParameter("q")));
            request.getRequestDispatcher("/WEB-INF/views/students.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Student list could not be loaded " + edu.alliance.library.util.Database.safeDiagnostic(e));
            request.setAttribute("error", "Student records are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String fullName = InputValidator.clean(request.getParameter("fullName"));
            String email = InputValidator.normalize(request.getParameter("email"));
            String studentId = InputValidator.clean(request.getParameter("studentId"));
            String department = InputValidator.clean(request.getParameter("department"));
            String phone = InputValidator.clean(request.getParameter("phone"));
            if (fullName.isBlank() || studentId.isBlank() || department.isBlank() || phone.isBlank() || !InputValidator.validEmail(email))
                throw new IllegalArgumentException("Enter valid values for all student fields.");
            students.update(Long.parseLong(request.getParameter("userId")), Long.parseLong(request.getParameter("recordId")),
                    fullName, email, studentId, department, phone);
            request.getSession().setAttribute("success", "Student details updated.");
            response.sendRedirect(request.getContextPath() + "/app/students");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Student update failed " + edu.alliance.library.util.Database.safeDiagnostic(e));
            request.setAttribute("error", "Student details could not be updated. Check for duplicate email or student ID.");
            load(request, response);
        }
    }
}
