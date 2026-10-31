// Initialize Dark Theme on load (prevents theme flashing)
(function() {
  const savedTheme = localStorage.getItem("theme");
  if (savedTheme === "dark") {
    document.body.classList.add("dark");
  } else {
    document.body.classList.remove("dark");
  }
})();

const NAV_ITEMS = [
  { href: "dashboard.html", label: "Dashboard", key: "dashboard", roles: ["Admin", "Doctor", "Receptionist", "Pharmacist"] },
  { href: "patient-dashboard.html", label: "My Health Record", key: "patient-dashboard", roles: ["Patient"] },
  { href: "appointments.html", label: "Appointments", key: "appointments", roles: ["Admin", "Doctor", "Receptionist"] },
  { href: "patients.html", label: "Patients", key: "patients", roles: ["Admin", "Doctor", "Receptionist"] },
  { href: "patient-history.html", label: "Patient History", key: "patient-history", roles: ["Admin", "Doctor"] },
  { href: "doctors.html", label: "Doctors", key: "doctors", roles: ["Admin", "Doctor", "Receptionist"] },
  { href: "prescriptions.html", label: "Prescriptions", key: "prescriptions", roles: ["Admin", "Doctor", "Pharmacist"] },
  { href: "prescription-ocr.html", label: "AI Prescription OCR", key: "prescription-ocr", roles: ["Admin", "Doctor"] },
  { href: "prescription-items.html", label: "Prescription Items", key: "prescription-items", roles: ["Admin", "Doctor", "Pharmacist"] },
  { href: "medicines.html", label: "Medicines", key: "medicines", roles: ["Admin", "Pharmacist", "Doctor"] },
  { href: "inventory.html", label: "Inventory (Stock)", key: "inventory", roles: ["Admin", "Pharmacist"] },
  { href: "suppliers.html", label: "Suppliers", key: "suppliers", roles: ["Admin", "Pharmacist"] },
  { href: "purchase-orders.html", label: "Purchase Orders", key: "purchase-orders", roles: ["Admin", "Pharmacist"] },
  { href: "ai-dashboard.html", label: "AI Insights", key: "ai-dashboard", roles: ["Admin", "Pharmacist", "Doctor"] },
  { href: "bills.html", label: "Bills", key: "bills", roles: ["Admin", "Receptionist"] },
  { href: "bill-items.html", label: "Bill Items", key: "bill-items", roles: ["Admin", "Receptionist"] },
  { href: "notifications.html", label: "Notification Center", key: "notifications", roles: ["Admin", "Doctor", "Receptionist", "Pharmacist"] },
  { href: "reports.html", label: "Reports", key: "reports", roles: ["Admin"] },
  { href: "audit-logs.html", label: "Audit Logs", key: "audit-logs", roles: ["Admin"] },
  { href: "users.html", label: "Users / Staff", key: "users", roles: ["Admin"] },
];

function toggleGlobalTheme() {
  const checkbox = document.getElementById("themeToggleCheckbox");
  if (checkbox && checkbox.checked) {
    document.body.classList.add("dark");
    localStorage.setItem("theme", "dark");
  } else {
    document.body.classList.remove("dark");
    localStorage.setItem("theme", "light");
  }
}

async function updateNotificationBell() {
  const bell = document.getElementById("topbarNotifBadge");
  if (!bell) return;
  try {
    const data = await Api.get("/notifications/unread-count");
    if (data && data.unreadCount > 0) {
      bell.textContent = data.unreadCount > 99 ? "99+" : data.unreadCount;
      bell.style.display = "inline-flex";
    } else {
      bell.style.display = "none";
    }
  } catch (e) {
    bell.style.display = "none";
  }
}

function renderSidebar(activeKey) {
  const mount = document.getElementById("sidebar");
  if (!mount) return;

  const user = currentUser();
  const role = user ? user.role : null;

  const visibleItems = NAV_ITEMS.filter(item => !role || item.roles.includes(role));

  const links = visibleItems.map(item => {
    const cls = item.key === activeKey ? "active" : "";
    return `<a href="${item.href}" class="${cls}">${item.label}</a>`;
  }).join("");

  const isDark = localStorage.getItem("theme") === "dark";

  mount.innerHTML = `
    <div class="brand">Care<span>Hub</span> <small style="font-size:11px; opacity:0.8; font-weight:normal;">v3.0</small></div>
    <nav>${links}</nav>
    <div class="user-box">
      Logged in as <strong>${user ? (user.name || user.username) : ""}</strong><br/>
      <span class="role">${user ? user.role : ""}</span><br/>
      <span class="logout-btn" onclick="logout()">Log out</span>
      <div class="theme-toggle-container">
        <span>Dark Mode</span>
        <label class="theme-switch">
          <input type="checkbox" id="themeToggleCheckbox" ${isDark ? "checked" : ""} onclick="toggleGlobalTheme()">
          <span class="theme-slider"></span>
        </label>
      </div>
    </div>
  `;

  // Attach Notification Bell to topbar if not already present
  const topbar = document.querySelector(".topbar");
  if (topbar && !document.getElementById("topbarNotifWrap")) {
    const wrap = document.createElement("div");
    wrap.id = "topbarNotifWrap";
    wrap.className = "topbar-actions";
    wrap.innerHTML = `
      <a href="notifications.html" class="notif-bell-btn" title="Notifications">
        🔔
        <span class="notif-badge" id="topbarNotifBadge" style="display:none;">0</span>
      </a>
    `;
    topbar.appendChild(wrap);
    updateNotificationBell();
  }

  // Redirect if unauthorized page
  if (role && activeKey) {
    const current = NAV_ITEMS.find(i => i.key === activeKey);
    if (current && !current.roles.includes(role)) {
      window.location.href = role === "Patient" ? "patient-dashboard.html" : "dashboard.html";
    }
  }
}
