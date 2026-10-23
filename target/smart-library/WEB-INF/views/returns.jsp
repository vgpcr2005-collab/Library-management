<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">CIRCULATION DESK</span><h1>Returns &amp; overdue</h1><p>Review active loans and record book returns.</p></div><a class="button button-primary" href="${pageContext.request.contextPath}/app/issue">＋ Issue a book</a></section>
<section class="panel filter-panel"><form class="filter-form" method="get" action="${pageContext.request.contextPath}/app/return"><label class="search-field"><span>⌕</span><input type="search" name="q" value="<c:out value='${param.q}'/>" placeholder="Search by title, student name or ID"></label><button class="button button-outline">Search loans</button></form></section>
<section class="panel"><div class="panel-heading"><div><span class="eyebrow">ACTIVE TRANSACTIONS</span><h2>Books currently on loan <span class="count-pill">${fn:length(issues)}</span></h2></div></div>
  <c:choose><c:when test="${empty issues}"><div class="empty-state"><span>✓</span><strong>No active loans</strong><p>There are no books waiting to be returned.</p></div></c:when><c:otherwise>
  <div class="table-wrap"><table><thead><tr><th>Book</th><th>Student</th><th>Due date</th><th>Fine to date</th><th>Action</th></tr></thead><tbody>
  <c:forEach items="${issues}" var="issue"><tr class="${issue.overdueDays gt 0 ? 'row-overdue' : ''}">
    <td><strong><c:out value="${issue.title}"/></strong><small class="table-sub">ISBN <c:out value="${issue.isbn}"/></small></td>
    <td><strong><c:out value="${issue.studentName}"/></strong><small class="table-sub"><c:out value="${issue.studentId}"/></small></td>
    <td><c:out value="${issue.dueDate}"/><c:if test="${issue.overdueDays gt 0}"><small class="table-sub text-danger"><c:out value="${issue.overdueDays}"/> days overdue</small></c:if></td>
    <td><c:choose><c:when test="${issue.overdueDays gt 0}"><span class="badge badge-red">Overdue</span></c:when><c:otherwise><span class="badge badge-green">On time</span></c:otherwise></c:choose></td>
    <td><form method="post" action="${pageContext.request.contextPath}/app/return" data-confirm="Record this return? Any overdue fine will be calculated automatically."><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="issueId" value="${issue.id}"><button class="button button-primary button-small">Record return</button></form></td>
  </tr></c:forEach></tbody></table></div>
  <p class="table-footnote">Fine is calculated at the configured daily rate when the return is recorded.</p>
  </c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
