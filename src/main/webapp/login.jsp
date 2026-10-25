<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#173b32">
  <title>Sign in | Alliance University Library</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="app-body login-page">
<div class="login-atmosphere" aria-hidden="true"><span></span><span></span><span></span></div>
<main class="login-layout">
  <section class="login-story" aria-label="Alliance University Library">
    <span class="login-story-mark">AU</span>
    <span class="eyebrow">A PLACE TO DISCOVER</span>
    <h2>Open a book.<br><span>Open a new world.</span></h2>
    <p>Find your next idea in a collection built for curious minds, thoughtful study, and lifelong learning.</p>
    <div class="book-illustration" aria-hidden="true">
      <span class="book-orbit book-orbit-one"></span><span class="book-orbit book-orbit-two"></span>
      <span class="book-shape book-shape-back"></span><span class="book-shape book-shape-front"></span>
      <span class="book-page"></span><span class="book-spark book-spark-one"></span><span class="book-spark book-spark-two"></span>
    </div>
    <span class="login-story-caption">READ · LEARN · GROW</span>
  </section>
  <section class="auth-card login-card">
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
  </section>
</main>
</body>
</html>
