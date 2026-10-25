(() => {
  "use strict";

  document.addEventListener("DOMContentLoaded", () => {
    const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
    const reduceMotion = motionPreference.matches;
    if (!motionPreference.matches && "IntersectionObserver" in window) {
      const motionTargets = document.querySelectorAll(
        ".welcome-row, .stat-card, .dashboard-grid > *, .panel, .workflow-card, .workflow-aside, .concept-card, .about-cards > *"
      );
      const observer = new IntersectionObserver((entries, currentObserver) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("motion-visible");
            currentObserver.unobserve(entry.target);
          }
        });
      }, { threshold: 0.08, rootMargin: "0px 0px -24px 0px" });

      motionTargets.forEach((element, index) => {
        element.classList.add("motion-pending");
        element.style.setProperty("--motion-order", String(index % 5));
        observer.observe(element);
      });

      const dashboard = document.querySelector(".stat-grid");
      if (dashboard) {
        const countObserver = new IntersectionObserver((entries, currentObserver) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            const value = entry.target;
            const target = Number(value.textContent.trim());
            if (!Number.isFinite(target) || target < 1) {
              currentObserver.unobserve(value);
              return;
            }
            const start = performance.now();
            const duration = 850;
            const count = (now) => {
              const progress = Math.min((now - start) / duration, 1);
              const eased = 1 - Math.pow(1 - progress, 3);
              value.textContent = String(Math.round(target * eased));
              if (progress < 1) window.requestAnimationFrame(count);
            };
            window.requestAnimationFrame(count);
            currentObserver.unobserve(value);
          });
        }, { threshold: 0.45 });
        dashboard.querySelectorAll(".stat-card strong").forEach((value) => countObserver.observe(value));
      }
    }

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
              { label: "Issues", data: monthly.map((item) => Number(item.dataset.issues)), borderColor: "#286453", backgroundColor: "rgba(40,100,83,.09)", tension: .32, fill: true },
              { label: "Returns", data: monthly.map((item) => Number(item.dataset.returns)), borderColor: "#b28a4a", backgroundColor: "rgba(178,138,74,.09)", tension: .32, fill: true }
            ]
          },
          options: { responsive: true, maintainAspectRatio: false, animation: reduceMotion ? false : { duration: 1100, easing: "easeOutQuart" }, plugins: { legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 7, font: { size: 10 } } } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: "#edf0f4" } }, x: { grid: { display: false } } } }
        });
      }
      const categories = [...document.querySelectorAll("#category-data span")];
      const categoryCanvas = document.querySelector("#category-chart");
      if (categoryCanvas && categories.length) {
        new Chart(categoryCanvas, {
          type: "doughnut",
          data: {
            labels: categories.map((item) => item.dataset.label),
            datasets: [{ data: categories.map((item) => Number(item.dataset.value)), backgroundColor: ["#214c40", "#b28a4a", "#6f9486", "#a8b9a7", "#8f7058", "#aeb9ad"], borderWidth: 2, borderColor: "#fff" }]
          },
          options: { responsive: true, maintainAspectRatio: false, cutout: "66%", animation: reduceMotion ? false : { duration: 1250, easing: "easeOutQuart", animateRotate: true, animateScale: true }, plugins: { legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 7, padding: 14, font: { size: 9 } } } } }
        });
      }
    }
  });
})();
