<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">ACCOUNT SECURITY</span><h1>My account</h1><p>Update your staff password regularly and keep it private.</p></div></section>
<section class="panel account-panel">
  <span class="eyebrow">CHANGE PASSWORD</span><h2><c:out value="${sessionScope.staff.fullName}"/></h2>
  <form method="post" action="${pageContext.request.contextPath}/app/account" class="form-grid account-form">
    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
    <label for="currentPassword">Current password</label><input id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" required>
    <label for="newPassword">New password</label><input id="newPassword" name="newPassword" type="password" minlength="14" maxlength="128" autocomplete="new-password" required>
    <label for="confirmPassword">Confirm new password</label><input id="confirmPassword" name="confirmPassword" type="password" minlength="14" maxlength="128" autocomplete="new-password" required>
    <p class="form-help">Use at least 14 characters. Your new password is stored as a salted, slow hash.</p>
    <button class="button button-primary" type="submit">Update password</button>
  </form>
</section>
<%@ include file="shell-bottom.jspf" %>
