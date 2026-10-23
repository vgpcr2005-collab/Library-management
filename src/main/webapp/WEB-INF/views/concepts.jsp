<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">ACADEMIC DEMONSTRATION</span><h1>Java concepts</h1><p>Five required concepts applied in the running application.</p></div></section>
<section class="concept-grid">
  <article class="panel concept-card"><span class="concept-number">01</span><span class="eyebrow">INHERITANCE</span><h2>User → Student / Librarian</h2><p><code>User</code> stores shared identity details. <code>Student</code> and <code>Librarian</code> extend it with type-specific information used in library records.</p><pre><code>public final class Student extends User {
  private final String studentId;
}</code></pre><small>Used by: <code>model/User.java</code>, <code>model/Student.java</code>, <code>model/Librarian.java</code></small></article>
  <article class="panel concept-card"><span class="concept-number">02</span><span class="eyebrow">POLYMORPHISM</span><h2>LibraryItem → Book → ReferenceBook</h2><p>The DAO returns a <code>Book</code> instance or a <code>ReferenceBook</code>. The catalogue calls <code>getDetails()</code> on a shared book reference; Java dispatches to the appropriate override at runtime.</p><pre><code>LibraryItem item = referenceOnly
    ? new ReferenceBook(...)
    : new Book(...);
String details = item.getDetails();</code></pre><small>Used by: <code>model/LibraryItem.java</code>, <code>model/Book.java</code>, <code>model/ReferenceBook.java</code></small></article>
  <article class="panel concept-card"><span class="concept-number">03</span><span class="eyebrow">ARRAYS</span><h2>Fixed search-field labels</h2><p>A fixed-size array records the supported discovery fields, providing a compact, bounded configuration while the actual catalogue remains database-backed.</p><pre><code>private static final String[] SEARCH_FIELDS = {
  "title", "author", "ISBN", "category"
};</code></pre><small>Used by: <code>service/LibraryService.java</code></small></article>
  <article class="panel concept-card"><span class="concept-number">04</span><span class="eyebrow">STRINGS</span><h2>Normalization &amp; validation</h2><p>Trim whitespace, normalize case with <code>Locale.ROOT</code>, validate email/ISBN formats, and bind all database values with prepared statements.</p><pre><code>String normalize(String value) {
  return clean(value).toLowerCase(Locale.ROOT);
}</code></pre><small>Used by: <code>util/InputValidator.java</code></small></article>
  <article class="panel concept-card"><span class="concept-number">05</span><span class="eyebrow">ENCAPSULATION</span><h2>Validated domain state</h2><p>Fields are private and immutable where possible. Constructors validate required properties; business methods protect copy counts against invalid inventory.</p><pre><code>public void setCopyCounts(int total, int available) {
  validateCopies(total, available);
  this.totalCopies = total;
  this.availableCopies = available;
}</code></pre><small>Used by: <code>model/Book.java</code> and all domain models</small></article>
</section>
<%@ include file="shell-bottom.jspf" %>
