<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="shell-top.jspf" %>
<section class="welcome-row"><div><span class="eyebrow">LIBRARY CONFIGURATION</span><h1>Settings</h1><p>Manage loan rules used by issue and return transactions.</p></div></section>
<section class="settings-layout"><article class="panel settings-panel">
  <div class="panel-heading"><div><span class="eyebrow">LENDING POLICY</span><h2>Circulation rules</h2></div></div>
  <form method="post" action="${pageContext.request.contextPath}/app/settings" class="form-stack" data-validate>
    <input type="hidden" name="csrfToken" value="${csrfToken}">
    <label for="loanDays">Loan period <span class="field-hint">Days</span></label><input id="loanDays" name="loanDays" type="number" min="1" max="365" required value="${settings.loan_days}">
    <label for="finePerDay">Overdue fine <span class="field-hint">₹ per day</span></label><input id="finePerDay" name="finePerDay" type="number" min="0" max="10000" step="0.01" required value="${settings.fine_per_day}">
    <label for="borrowingLimit">Borrowing limit <span class="field-hint">Active books per student</span></label><input id="borrowingLimit" name="borrowingLimit" type="number" min="1" max="100" required value="${settings.borrowing_limit}">
    <button class="button button-primary" type="submit">Save library settings</button>
  </form>
</article><aside class="panel settings-note"><span class="stat-icon gold">i</span><h2>Applied to new transactions</h2><p>Changes affect the due dates of new loans and fine calculations on future returns. Existing due dates remain unchanged.</p><div class="aside-callout"><strong>Data-backed configuration</strong><p>These values are stored in the configured database and read by the circulation service.</p></div></aside></section>
<%@ include file="shell-bottom.jspf" %>
