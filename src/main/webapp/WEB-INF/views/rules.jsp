<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">STUDENT &amp; FACULTY INFORMATION</span><h1>Library rules</h1><p>Current circulation rules and what to expect when you request, borrow, or return a book.</p></div></section>
<section class="dashboard-grid">
  <article class="panel">
    <div class="panel-heading"><div><span class="eyebrow">CURRENT LOAN POLICY</span><h2>Borrowing limits</h2></div></div>
    <ul class="rules-list">
      <li>Standard loans are due <strong>${settings.loan_days} days</strong> after issue.</li>
      <li>Each student may have up to <strong>${settings.borrowing_limit} active books</strong> at a time.</li>
      <li>Overdue books incur a fine of <strong>₹${settings.fine_per_day} per day</strong>, calculated when staff records the return.</li>
      <li>Reference-only books are for library use and cannot be borrowed.</li>
    </ul>
  </article>
  <article class="panel">
    <div class="panel-heading"><div><span class="eyebrow">REQUESTS &amp; RETURNS</span><h2>How the process works</h2></div></div>
    <ul class="rules-list">
      <li><strong>Borrow requests:</strong> A request marked Pending is waiting for library staff approval. It is not yet a loan; no due date is assigned until it is approved.</li>
      <li><strong>Renewals:</strong> Send a renewal request before the due date. Online renewal is unavailable for overdue loans, and a loan can only be renewed once.</li>
      <li><strong>Returns:</strong> Submit a return request and bring the physical book to the library desk. It remains on loan until staff receives it and confirms the return.</li>
      <li>Staff confirmation updates the loan and available-copy count and records any applicable overdue fine.</li>
      <li>Ask library staff for current desk hours and any guidance about paying recorded fines.</li>
    </ul>
  </article>
</section>
<div class="notice notice-info" role="note">These loan days, limits, and fine rates are shown from the library's current settings. Ask staff if you need help with a request or return.</div>
<%@ include file="shell-bottom.jspf" %>
