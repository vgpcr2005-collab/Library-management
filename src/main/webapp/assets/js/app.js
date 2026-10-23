(() => {
  "use strict";

  document.addEventListener("DOMContentLoaded", () => {
      document.querySelectorAll("[data-password-toggle]").forEach((toggle) => {
          toggle.addEventListener("click", () => {
              const input = document.getElementById(toggle.dataset.passwordToggle);
              if (!input) return;
              const visible = input.type === "password";
              input.type = visible ? "text" : "password";
              toggle.textContent = visible ? "Hide" : "Show";
              toggle.setAttribute("aria-label", visible ? "Hide password" : "Show password");
          });
      });

    const menuToggle = document.querySelector("[data-menu-toggle]");
    const sidebar = document.querySelector("#sidebar");
    if (menuToggle && sidebar) {
      menuToggle.addEventListener("click", () => {
        const open = sidebar.classList.toggle("is-open");
        menuToggle.setAttribute("aria-expanded", String(open));
      });
    }

    document.querySelectorAll("[data-validate]").forEach((form) => {
      form.addEventListener("submit", (event) => {
        if (!form.reportValidity()) {
          event.preventDefault();
          return;
        }
        const submit = form.querySelector('button[type="submit"]');
        if (submit) {
          submit.disabled = true;
          submit.dataset.originalText = submit.textContent;
          submit.textContent = "Please wait…";
        }
      });
    });

    document.querySelectorAll("[data-confirm]").forEach((form) => {
      form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
      });
    });

    if (window.Chart) {
      const monthly = [...document.querySelectorAll("#monthly-data span")];
      const activityCanvas = document.querySelector("#activity-chart");
      if (activityCanvas && monthly.length) {
        new Chart(activityCanvas, {
          type: "line",
          data: {
            labels: monthly.map((item) => item.dataset.label),
            datasets: [
              { label: "Issues", data: monthly.map((item) => Number(item.dataset.issues)), borderColor: "#223d62", backgroundColor: "rgba(34,61,98,.08)", tension: .32, fill: true },
              { label: "Returns", data: monthly.map((item) => Number(item.dataset.returns)), borderColor: "#c6a564", backgroundColor: "rgba(198,165,100,.08)", tension: .32, fill: true }
            ]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 7, font: { size: 10 } } } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: "#edf0f4" } }, x: { grid: { display: false } } } }
        });
      }
      const categories = [...document.querySelectorAll("#category-data span")];
      const categoryCanvas = document.querySelector("#category-chart");
      if (categoryCanvas && categories.length) {
        new Chart(categoryCanvas, {
          type: "doughnut",
          data: {
            labels: categories.map((item) => item.dataset.label),
            datasets: [{ data: categories.map((item) => Number(item.dataset.value)), backgroundColor: ["#1d3555", "#c6a564", "#7893b2", "#6a9a85", "#b47a73", "#a6afbd"], borderWidth: 2, borderColor: "#fff" }]
          },
          options: { responsive: true, maintainAspectRatio: false, cutout: "66%", plugins: { legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 7, padding: 14, font: { size: 9 } } } } }
        });
      }
    }
  });
})();
