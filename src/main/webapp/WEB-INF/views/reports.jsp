<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">INSIGHTS &amp; EXPORTS</span><h1>Library reports</h1><p>Filter current library data and download a CSV for analysis.</p></div></section>
<section class="panel filter-panel"><form class="filter-form" method="get" action="${pageContext.request.contextPath}/app/reports">
  <label for="reportType" class="visually-hidden">Report type</label><select id="reportType" name="type"><option value="issued" ${type eq 'issued' ? 'selected' : ''}>Issued books</option><option value="overdue" ${type eq 'overdue' ? 'selected' : ''}>Overdue books</option><option value="available" ${type eq 'available' ? 'selected' : ''}>Available books</option><option value="popular" ${type eq 'popular' ? 'selected' : ''}>Most borrowed books</option><option value="monthly" ${type eq 'monthly' ? 'selected' : ''}>Monthly activity</option><option value="history" ${type eq 'history' ? 'selected' : ''}>Student borrowing history</option></select>
  <input type="text" name="studentId" value="<c:out value='${param.studentId}'/>" placeholder="Student ID for history" aria-label="Student ID">
  <button class="button button-outline">View report</button><a class="button button-primary" href="${pageContext.request.contextPath}/app/reports?type=${type}&amp;format=csv&amp;studentId=<c:out value='${param.studentId}'/>">Export CSV ↓</a>
</form></section>
<section class="panel"><div class="panel-heading"><div><span class="eyebrow">DATABASE REPORT</span><h2><c:out value="${type}"/> <span class="count-pill">${fn:length(rows)} rows</span></h2></div></div>
  <c:choose><c:when test="${empty rows}"><div class="empty-state"><span>▥</span><strong>No records for this report</strong><p>Try another report type or check back when library activity is available.</p></div></c:when><c:otherwise>
  <div class="table-wrap"><table><thead><tr><c:forEach items="${rows[0]}" var="cell"><th><c:out value="${fn:replace(cell.key, '_', ' ')}"/></th></c:forEach></tr></thead><tbody>
  <c:forEach items="${rows}" var="row"><tr><c:forEach items="${row}" var="cell"><td><c:out value="${cell.value}"/></td></c:forEach></tr></c:forEach>
  </tbody></table></div></c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
