<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="panel error-panel"><span class="stat-icon red">!</span><span class="eyebrow">TEMPORARILY UNAVAILABLE</span><h1>We couldn't load this page.</h1><p><c:out value="${error}"/></p><a class="button button-primary" href="${pageContext.request.contextPath}/app/dashboard">Return to dashboard</a></section>
<%@ include file="shell-bottom.jspf" %>
