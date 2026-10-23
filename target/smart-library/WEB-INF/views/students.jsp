<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">MEMBER SERVICES</span><h1>Students</h1><p>Find members and maintain their university library profile.</p></div></section>
<section class="panel filter-panel"><form class="filter-form" method="get" action="${pageContext.request.contextPath}/app/students">
  <label class="search-field"><span>⌕</span><input type="search" name="q" value="<c:out value='${param.q}'/>" placeholder="Search by student ID or name"></label><button class="button button-outline">Search students</button>
</form></section>
<section class="panel">
  <div class="panel-heading"><div><span class="eyebrow">REGISTERED MEMBERS</span><h2>Student directory <span class="count-pill">${fn:length(students)}</span></h2></div></div>
  <c:choose><c:when test="${empty students}"><div class="empty-state"><span>♙</span><strong>No students found</strong><p>Try a different name or student ID.</p></div></c:when><c:otherwise>
  <div class="table-wrap"><table><thead><tr><th>Student</th><th>Student ID</th><th>Department</th><th>Contact</th><th>Profile</th></tr></thead><tbody>
  <c:forEach items="${students}" var="student"><tr>
    <td><strong><c:out value="${student.fullName}"/></strong><small class="table-sub"><c:out value="${student.email}"/></small></td>
    <td><span class="id-chip"><c:out value="${student.studentId}"/></span></td>
    <td><c:out value="${student.department}"/></td><td><c:out value="${student.phone}"/></td>
    <td><details class="row-actions"><summary class="text-link">Edit profile</summary>
      <form class="row-edit-form" method="post" action="${pageContext.request.contextPath}/app/students" data-validate>
        <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="userId" value="${student.id}"><input type="hidden" name="recordId" value="${student.studentRecordId}">
        <label>Full name<input name="fullName" value="<c:out value='${student.fullName}'/>" required></label><label>Email<input name="email" type="email" value="<c:out value='${student.email}'/>" required></label>
        <label>Student ID<input name="studentId" value="<c:out value='${student.studentId}'/>" required></label><label>Department<input name="department" value="<c:out value='${student.department}'/>" required></label>
        <label>Phone<input name="phone" value="<c:out value='${student.phone}'/>" required></label><button class="button button-primary button-small">Save profile</button>
      </form>
    </details></td>
  </tr></c:forEach></tbody></table></div>
  </c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
