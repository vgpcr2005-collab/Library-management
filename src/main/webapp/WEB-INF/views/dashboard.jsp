<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row">
  <div><span class="eyebrow">LIBRARY OVERVIEW</span><h1>Welcome to the library.</h1><p>Here is what is happening across your library today.</p></div>
  <a class="button button-primary" href="${pageContext.request.contextPath}/app/issue">＋ Issue a book</a>
</section>
<section class="stat-grid" aria-label="Library statistics">
  <article class="stat-card"><span class="stat-icon navy">▤</span><div><span>Total book copies</span><strong><c:out value="${stats.totalBooks}"/></strong><small>In the collection</small></div></article>
  <article class="stat-card"><span class="stat-icon green">✓</span><div><span>Available copies</span><strong><c:out value="${stats.availableCopies}"/></strong><small>Ready to borrow</small></div></article>
  <article class="stat-card"><span class="stat-icon blue">♙</span><div><span>Registered students</span><strong><c:out value="${stats.students}"/></strong><small>Active library members</small></div></article>
  <article class="stat-card"><span class="stat-icon gold">↗</span><div><span>Currently issued</span><strong><c:out value="${stats.issued}"/></strong><small>Active loans</small></div></article>
  <article class="stat-card stat-alert"><span class="stat-icon red">!</span><div><span>Overdue books</span><strong><c:out value="${stats.overdue}"/></strong><small>Need follow-up</small></div></article>
</section>
<section class="dashboard-grid">
  <article class="panel chart-panel">
    <div class="panel-heading"><div><span class="eyebrow">LIBRARY ACTIVITY</span><h2>Issues &amp; returns</h2></div><span class="badge badge-muted">Last 6 months</span></div>
    <div class="chart-wrap"><canvas id="activity-chart" aria-label="Monthly issues and returns chart"></canvas></div>
    <div class="chart-data hidden" id="monthly-data"><c:forEach items="${stats.monthly}" var="m"><span data-label="${fn:escapeXml(m.label)}" data-issues="${m.issues}" data-returns="${m.returns}"></span></c:forEach></div>
  </article>
  <article class="panel chart-panel">
    <div class="panel-heading"><div><span class="eyebrow">COLLECTION MIX</span><h2>Popular categories</h2></div></div>
    <div class="category-chart-wrap"><canvas id="category-chart" aria-label="Books by category chart"></canvas></div>
    <div class="chart-data hidden" id="category-data"><c:forEach items="${stats.categories}" var="category"><span data-label="${fn:escapeXml(category.label)}" data-value="${category.value}"></span></c:forEach></div>
  </article>
</section>
<section class="dashboard-grid dashboard-grid-bottom">
  <article class="panel">
    <div class="panel-heading"><div><span class="eyebrow">RECENT ACTIVITY</span><h2>Recently issued books</h2></div><a class="text-link" href="${pageContext.request.contextPath}/app/return">Manage loans →</a></div>
    <c:choose><c:when test="${empty stats.recent}"><div class="empty-state"><span>▤</span><strong>No issue activity yet</strong><p>New loans will appear here.</p></div></c:when><c:otherwise>
      <div class="table-wrap"><table><thead><tr><th>Book</th><th>Student</th><th>Issued</th><th>Status</th></tr></thead><tbody>
      <c:forEach items="${stats.recent}" var="loan"><tr><td><strong><c:out value="${loan.title}"/></strong></td><td><c:out value="${loan.student}"/><small class="table-sub"><c:out value="${loan.studentId}"/></small></td><td><c:out value="${loan.issueDate}"/></td><td><span class="badge ${loan.returned ? 'badge-green' : 'badge-blue'}">${loan.returned ? 'Returned' : 'On loan'}</span></td></tr></c:forEach>
      </tbody></table></div>
    </c:otherwise></c:choose>
  </article>
  <article class="panel popular-panel">
    <div class="panel-heading"><div><span class="eyebrow">READER FAVOURITES</span><h2>Most borrowed</h2></div></div>
    <c:choose><c:when test="${empty stats.popular}"><div class="empty-state"><strong>No titles to rank yet</strong><p>Borrowing activity creates the leaderboard.</p></div></c:when><c:otherwise>
    <ol class="leaderboard"><c:forEach items="${stats.popular}" var="book" varStatus="loop"><li><span class="rank">${loop.count}</span><div><strong><c:out value="${book.title}"/></strong><small><c:out value="${book.author}"/></small></div><span class="leader-count"><c:out value="${book.borrows}"/></span></li></c:forEach></ol>
    </c:otherwise></c:choose>
  </article>
</section>
<%@ include file="shell-bottom.jspf" %>
