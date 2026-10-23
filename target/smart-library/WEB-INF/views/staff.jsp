<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">ADMINISTRATION</span><h1>Staff accounts</h1><p>Create and disable librarian accounts. Only administrators can manage staff access.</p></div></section>
<section class="staff-grid">
  <article class="panel"><div class="panel-heading"><div><span class="eyebrow">AUTHORIZED USERS</span><h2>Library staff</h2></div></div>
    <c:choose><c:when test="${empty staffAccounts}">
      <div class="empty-state"><strong>No staff accounts</strong><p>Create a librarian account to grant staff access.</p></div>
    </c:when><c:otherwise>
      <div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Access</th></tr></thead><tbody>
      <c:forEach items="${staffAccounts}" var="account"><tr>
        <td><c:out value="${account.name}"/></td><td><c:out value="${account.email}"/></td><td><c:out value="${account.role}"/></td>
        <td><span class="status-badge ${account.enabled ? 'status-available' : 'status-unavailable'}"><c:out value="${account.enabled ? 'Enabled' : 'Disabled'}"/></span></td>
        <td><c:if test="${account.role eq 'LIBRARIAN'}"><form method="post" action="${pageContext.request.contextPath}/app/staff" class="inline-form">
          <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="toggle">
          <input type="hidden" name="accountId" value="${account.id}"><input type="hidden" name="enabled" value="${not account.enabled}">
          <button class="button button-outline button-small" type="submit"><c:out value="${account.enabled ? 'Disable' : 'Enable'}"/></button></form></c:if></td>
      </tr></c:forEach></tbody></table></div>
    </c:otherwise></c:choose>
  </article>
  <article class="panel staff-create"><span class="eyebrow">NEW ACCOUNT</span><h2>Add a librarian</h2>
    <form method="post" action="${pageContext.request.contextPath}/app/staff" class="form-grid">
      <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="create">
      <label for="fullName">Full name</label><input id="fullName" name="fullName" maxlength="120" autocomplete="name" required>
      <label for="staffEmail">University email</label><input id="staffEmail" name="email" type="email" maxlength="254" autocomplete="email" required>
      <label for="staffPassword">Temporary password</label><input id="staffPassword" name="password" type="password" minlength="14" maxlength="128" autocomplete="new-password" required>
      <p class="form-help">Use at least 14 characters. Ask the librarian to change it after signing in.</p>
      <button class="button button-primary" type="submit">Create librarian account</button>
    </form>
  </article>
</section>
<%@ include file="shell-bottom.jspf" %>
