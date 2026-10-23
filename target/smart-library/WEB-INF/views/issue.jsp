<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">CIRCULATION DESK</span><h1>Issue a book</h1><p>Check out an available title to a registered student.</p></div><a class="button button-outline" href="${pageContext.request.contextPath}/app/return">Go to returns →</a></section>
<section class="workflow-grid">
  <article class="panel workflow-card"><span class="workflow-number">01</span><span class="eyebrow">SELECT MEMBER</span><h2>Choose a student</h2><p>Select a registered student. The borrowing limit is checked automatically.</p>
    <form class="form-stack" method="post" action="${pageContext.request.contextPath}/app/issue" data-validate>
      <input type="hidden" name="csrfToken" value="${csrfToken}">
      <label for="studentRecordId">Student</label><select name="studentRecordId" id="studentRecordId" required><option value="">Select a student</option><c:forEach items="${students}" var="student"><option value="${student.studentRecordId}"><c:out value="${student.fullName}"/> · <c:out value="${student.studentId}"/></option></c:forEach></select>
      <label for="bookId">Book</label><select name="bookId" id="bookId" required><option value="">Select an available book</option><c:forEach items="${books}" var="book"><option value="${book.id}"><c:out value="${book.title}"/> — <c:out value="${book.author}"/> (<c:out value="${book.availableCopies}"/> available)</option></c:forEach></select>
      <div class="form-note">Issue date and due date are recorded automatically from library settings.</div>
      <button class="button button-primary" type="submit" ${empty books or empty students ? 'disabled' : ''}>Confirm issue <span>→</span></button>
    </form>
  </article>
  <aside class="panel workflow-aside"><span class="eyebrow">BEFORE YOU ISSUE</span><h2>Circulation guidelines</h2><ul class="guideline-list"><li><span>✓</span> The book must have an available copy.</li><li><span>✓</span> Reference books are for library use only.</li><li><span>✓</span> Student borrowing limits are enforced.</li><li><span>✓</span> Due dates use the configured loan period.</li></ul><div class="aside-callout"><strong>Need to adjust lending rules?</strong><p>Review loan periods, fines, and borrowing limits in Settings.</p><a class="text-link" href="${pageContext.request.contextPath}/app/settings">Open settings →</a></div></aside>
</section>
<%@ include file="shell-bottom.jspf" %>
