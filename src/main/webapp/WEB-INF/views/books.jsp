<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">COLLECTION MANAGEMENT</span><h1>Book catalogue</h1><p>Discover, manage, and monitor your library collection.</p></div></section>
<section class="panel filter-panel">
  <form class="filter-form" method="get" action="${pageContext.request.contextPath}/app/books">
    <label class="search-field"><span>⌕</span><input type="search" name="q" value="<c:out value='${param.q}'/>" placeholder="Search title, author, ISBN or category" aria-label="Search catalogue"></label>
    <select name="category" aria-label="Filter category"><option value="">All categories</option><c:forEach items="${categories}" var="category"><option value="<c:out value='${category}'/>" ${param.category eq category ? 'selected' : ''}><c:out value="${category}"/></option></c:forEach></select>
    <select name="availability" aria-label="Filter availability"><option value="">Any availability</option><option value="available" ${param.availability eq 'available' ? 'selected' : ''}>Available</option><option value="unavailable" ${param.availability eq 'unavailable' ? 'selected' : ''}>Unavailable</option></select>
    <button class="button button-outline" type="submit">Filter</button>
  </form>
  <small class="table-sub search-hint">Smart discovery searches: ${fn:join(searchFields, ', ')}.</small>
</section>
<c:if test="${sessionScope.account.role ne 'STUDENT'}"><section class="panel form-panel">
  <details><summary class="details-summary">＋ Add a book to the collection</summary>
    <form class="form-grid details-form" method="post" action="${pageContext.request.contextPath}/app/books" data-validate>
      <input type="hidden" name="csrfToken" value="${csrfToken}">
      <div class="field"><label>ISBN</label><input name="isbn" required pattern="([0-9]{9}[0-9Xx]|[0-9]{13})" placeholder="13-digit ISBN"></div>
      <div class="field"><label>Title</label><input name="title" required maxlength="240"></div>
      <div class="field"><label>Author</label><input name="author" required maxlength="160"></div>
      <div class="field"><label>Category</label><input name="category" required maxlength="100"></div>
      <div class="field"><label>Total copies</label><input name="totalCopies" type="number" required min="0" value="1"></div>
      <label class="check-row field-align"><input type="checkbox" name="referenceOnly"><span>Reference only (not borrowable)</span></label>
      <div class="field field-full"><button class="button button-primary" type="submit">Add book</button></div>
    </form>
  </details>
</section>
</c:if>
<section class="panel">
  <div class="panel-heading"><div><span class="eyebrow">COLLECTION</span><h2>Books <span class="count-pill">${fn:length(books)}</span></h2></div></div>
  <c:choose><c:when test="${empty books}"><div class="empty-state"><span>⌕</span><strong>No matching books</strong><p>Try changing your search or filters.</p></div></c:when><c:otherwise>
  <div class="table-wrap"><table><thead><tr><th>Book details</th><th>Category</th><th>Availability</th><th>Copies</th><th>${sessionScope.account.role eq 'STUDENT' ? 'Request' : 'Actions'}</th></tr></thead><tbody>
  <c:forEach items="${books}" var="book"><tr>
    <td><strong><c:out value="${book.title}"/></strong><small class="table-sub"><c:out value="${book.author}"/> · ISBN <c:out value="${book.isbn}"/></small><small class="table-sub"><c:out value="${book.details}"/></small></td>
    <td><span class="badge badge-muted"><c:out value="${book.category}"/></span></td>
    <td><span class="availability"><i class="${book.availableCopies gt 0 ? 'availability-in' : 'availability-out'}"></i><c:out value="${book.availableCopies gt 0 ? 'Available' : 'All on loan'}"/></span><c:if test="${book.availableCopies le 1 and not book.referenceOnly}"><small class="table-sub text-warning">Low stock</small></c:if></td>
    <td><strong><c:out value="${book.availableCopies}"/></strong> / <c:out value="${book.totalCopies}"/></td>
    <c:choose><c:when test="${sessionScope.account.role eq 'STUDENT'}"><td><c:if test="${not book.referenceOnly}"><form method="post" action="${pageContext.request.contextPath}/app/my-library" class="inline-form"><input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="action" value="borrow"><input type="hidden" name="bookId" value="${book.id}"><button class="button button-outline button-small" type="submit">Request</button></form></c:if></td></c:when><c:otherwise><td><details class="row-actions"><summary class="text-link">Edit</summary>
      <form class="row-edit-form" method="post" action="${pageContext.request.contextPath}/app/books" data-validate>
        <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="id" value="${book.id}">
        <label>ISBN<input name="isbn" value="<c:out value='${book.isbn}'/>" required></label><label>Title<input name="title" value="<c:out value='${book.title}'/>" required></label>
        <label>Author<input name="author" value="<c:out value='${book.author}'/>" required></label><label>Category<input name="category" value="<c:out value='${book.category}'/>" required></label>
        <label>Total copies<input name="totalCopies" type="number" min="0" value="${book.totalCopies}" required></label>
        <label class="check-row"><input type="checkbox" name="referenceOnly" ${book.referenceOnly ? 'checked' : ''}><span>Reference only</span></label>
        <button class="button button-primary button-small" type="submit">Save changes</button>
      </form>
    </details>
    <form method="post" action="${pageContext.request.contextPath}/app/books" class="inline-form" data-confirm="Remove this book from the catalogue? Books with borrowing history cannot be deleted.">
      <input type="hidden" name="csrfToken" value="${csrfToken}"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${book.id}">
      <button class="button button-danger button-small" type="submit">Delete</button>
    </form></td></c:otherwise></c:choose>
  </tr></c:forEach></tbody></table></div>
  </c:otherwise></c:choose>
</section>
<%@ include file="shell-bottom.jspf" %>
