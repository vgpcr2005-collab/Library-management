<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">STUDENT SERVICES</span><h1>My library</h1><p>Manage your profile and keep track of books issued to you.</p></div><a class="button button-primary" href="${pageContext.request.contextPath}/app/books">Browse books →</a></section>
<section class="dashboard-grid">
  <article class="panel form-panel"><div class="panel-heading"><div><span class="eyebrow">YOUR DETAILS</span><h2>Student profile</h2></div></div>
    <form class="form-stack" method="post" action="${pageContext.request.contextPath}/app/my-library" data-validate>
      <input type="hidden" name="csrfToken" value="${csrfToken}">
      <label>Full name<input name="fullName" maxlength="120" value="<c:out value='${student.fullName}'/>" required></label>
      <label>Email<input name="email" type="email" maxlength="254" value="<c:out value='${student.email}'/>" required></label>
      <label>Student ID<input value="<c:out value='${student.studentId}'/>" readonly></label>
      <label>Department<input name="department" maxlength="100" value="<c:out value='${student.department}'/>" required></label>
      <label>Phone<input name="phone" maxlength="24" value="<c:out value='${student.phone}'/>" required></label>
      <button class="button button-primary" type="submit">Save profile</button>
    </form>
  </article>
  <article class="panel"><div class="panel-heading"><div><span class="eyebrow">BORROWING HISTORY</span><h2>My loans <span class="count-pill">${fn:length(loans)}</span></h2></div></div>
    <c:choose><c:when test="${empty loans}"><div class="empty-state"><strong>No loans yet</strong><p>Browse the catalogue to find a book.</p></div></c:when><c:otherwise>
      <div class="table-wrap"><table><thead><tr><th>Book</th><th>Issued</th><th>Due</th><th>Status</th></tr></thead><tbody>
        <c:forEach items="${loans}" var="loan"><tr><td><strong><c:out value="${loan.title}"/></strong><small class="table-sub"><c:out value="${loan.author}"/></small></td><td><c:out value="${loan.issueDate}"/></td><td><c:out value="${loan.dueDate}"/></td><td><span class="badge ${loan.returned ? 'badge-green' : 'badge-blue'}">${loan.returned ? 'Returned' : 'On loan'}</span><c:if test="${not loan.returned}"><form class="inline-form" method="post" action="${pageContext.request.contextPath}/app/my-library"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="action" value="renew"><input type="hidden" name="issueId" value="${loan.id}"><button class="button button-outline button-small" type="submit">Request renewal</button></form></c:if></td></tr></c:forEach>
      </tbody></table></div>
    </c:otherwise></c:choose>
  </article>
</section>
<section class="panel"><div class="panel-heading"><div><span class="eyebrow">BOOK REQUESTS</span><h2>Request history <span class="count-pill">${fn:length(requests)}</span></h2></div><a class="text-link" href="${pageContext.request.contextPath}/app/books">Browse books →</a></div>
  <c:choose><c:when test="${empty requests}"><div class="empty-state"><strong>No requests yet</strong><p>Request an available title from the catalogue.</p></div></c:when><c:otherwise><div class="table-wrap"><table><thead><tr><th>Book</th><th>Type</th><th>Requested</th><th>Status</th></tr></thead><tbody><c:forEach items="${requests}" var="item"><tr><td><strong><c:out value="${item.title}"/></strong></td><td><c:out value="${item.requestType}"/></td><td><c:out value="${item.requestedAt}"/></td><td><span class="badge ${item.status eq 'APPROVED' ? 'badge-green' : item.status eq 'REJECTED' ? 'badge-muted' : 'badge-blue'}"><c:out value="${item.status}"/></span></td></tr></c:forEach></tbody></table></div></c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
