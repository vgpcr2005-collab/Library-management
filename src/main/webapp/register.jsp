<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Student registration | Alliance University Library</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
  <script defer src="${pageContext.request.contextPath}/assets/js/app.js"></script>
</head>
<body class="app-body">
<main class="auth-card auth-card-wide">
  <a class="brand" href="${pageContext.request.contextPath}/login"><span class="brand-mark">AU</span><span class="brand-copy"><strong>ALLIANCE</strong><small>UNIVERSITY</small></span></a>
  <span class="eyebrow">STUDENT SERVICES</span><h1>Create your account</h1>
  <p>Register to browse the catalogue and view your library loans. Faculty accounts are managed by library staff.</p>
  <c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
  <form class="form-grid" method="post" action="${pageContext.request.contextPath}/register" data-validate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <div class="field"><label for="fullName">Full name</label><input id="fullName" name="fullName" maxlength="120" required></div>
    <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" maxlength="254" autocomplete="email" required></div>
    <div class="field"><label for="studentId">Student ID</label><input id="studentId" name="studentId" maxlength="40" required></div>
    <div class="field"><label for="department">Department</label><input id="department" name="department" maxlength="100" required></div>
    <div class="field"><label for="phone">Phone</label><input id="phone" name="phone" maxlength="24" autocomplete="tel" required></div>
    <div class="field"><label for="password">Password (12+ characters)</label><input id="password" name="password" type="password" minlength="12" maxlength="128" autocomplete="new-password" required></div>
    <div class="field field-full"><button class="button button-primary" type="submit">Create student account</button></div>
  </form>
  <p class="auth-footer">Already registered? <a class="text-link" href="${pageContext.request.contextPath}/login">Sign in</a></p>
</main>
</body>
</html>
