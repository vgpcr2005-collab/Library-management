<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#101f37">
  <title>Staff sign in | Alliance University Library</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="login-page">
<main class="login-layout">
  <section class="login-brand-panel">
    <a class="brand login-brand" href="${pageContext.request.contextPath}/login">
      <span class="brand-mark">AU</span><span class="brand-copy"><strong>ALLIANCE</strong><small>UNIVERSITY</small></span>
    </a>
    <div>
      <span class="eyebrow">LIBRARY SERVICES</span>
      <h1>Knowledge opens every door.</h1>
      <p>Alliance University Smart Library Management System</p>
    </div>
    <span class="login-brand-foot">SECURE STAFF PORTAL</span>
  </section>
  <section class="login-form-panel">
    <div class="login-form-wrap">
      <span class="eyebrow">STAFF ACCESS</span>
      <h2>Welcome back</h2>
      <p>Sign in with your authorized librarian or administrator account.</p>
      <c:if test="${not empty error}"><div class="notice notice-error" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${staffConfigured eq false}">
        <div class="notice notice-error" role="alert">No staff administrator is configured. Set the one-time bootstrap administrator email and password in the server environment, then restart the website.</div>
      </c:if>
      <form method="post" action="${pageContext.request.contextPath}/login" class="login-form">
        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
        <label for="email">University email</label>
        <input id="email" name="email" type="email" autocomplete="username" maxlength="254" required autofocus>
        <label for="password">Password</label>
        <div class="password-field">
          <input id="password" name="password" type="password" autocomplete="current-password" maxlength="1024" required>
          <button class="password-toggle" type="button" data-password-toggle="password" aria-label="Show password">Show</button>
        </div>
        <button class="button button-primary button-wide" type="submit">Sign in securely</button>
      </form>
      <p class="login-help">Accounts are created by the library administrator. Public student registration is disabled.</p>
    </div>
  </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/app.js" defer></script>
</body>
</html>
