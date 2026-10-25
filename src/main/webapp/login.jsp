<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sign in | Alliance University Library</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="app-body">
<main class="auth-card">
  <a class="brand" href="${pageContext.request.contextPath}/login"><span class="brand-mark">AU</span><span class="brand-copy"><strong>ALLIANCE</strong><small>UNIVERSITY</small></span></a>
  <span class="eyebrow">SMART LIBRARY PORTAL</span><h1>Welcome back</h1>
  <p>Sign in as a student or faculty/library staff member.</p>
  <c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
  <form class="form-stack" method="post" action="${pageContext.request.contextPath}/login" data-validate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <label>Email<input name="email" type="email" autocomplete="username" maxlength="254" required></label>
    <label>Password<input name="password" type="password" autocomplete="current-password" required></label>
    <button class="button button-primary" type="submit">Sign in</button>
  </form>
  <p class="auth-footer">Student? <a class="text-link" href="${pageContext.request.contextPath}/register">Create a student account</a></p>
</main>
</body>
</html>
