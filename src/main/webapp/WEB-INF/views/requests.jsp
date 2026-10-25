<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">CIRCULATION</span><h1>Book requests</h1><p>Review student borrowing and renewal requests.</p></div></section>
<section class="panel"><div class="panel-heading"><div><span class="eyebrow">NEEDS REVIEW</span><h2>Pending requests <span class="count-pill">${fn:length(requests)}</span></h2></div></div>
  <c:choose><c:when test="${empty requests}"><div class="empty-state"><strong>No pending requests</strong><p>New student requests will appear here.</p></div></c:when><c:otherwise>
    <div class="table-wrap"><table><thead><tr><th>Student</th><th>Book</th><th>Request</th><th>Requested</th><th>Decision</th></tr></thead><tbody>
      <c:forEach items="${requests}" var="item"><tr><td><strong><c:out value="${item.studentName}"/></strong><small class="table-sub"><c:out value="${item.studentId}"/></small></td><td><strong><c:out value="${item.title}"/></strong><small class="table-sub"><c:out value="${item.author}"/></small></td><td><c:out value="${item.requestType}"/></td><td><c:out value="${item.requestedAt}"/></td><td>
        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/app/requests"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="requestId" value="${item.id}"><input type="hidden" name="decision" value="approve"><button class="button button-primary button-small" type="submit">Approve</button></form>
        <form class="inline-form" method="post" action="${pageContext.request.contextPath}/app/requests"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="requestId" value="${item.id}"><input type="hidden" name="decision" value="reject"><button class="button button-outline button-small" type="submit">Reject</button></form>
      </td></tr></c:forEach>
    </tbody></table></div>
  </c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
