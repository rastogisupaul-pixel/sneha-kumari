import { initializeApp } from "https://www.gstatic.com/firebasejs/11.3.0/firebase-app.js";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from "https://www.gstatic.com/firebasejs/11.3.0/firebase-auth.js";

let firebaseAuth = null;
let idToken = "";

const el = (id) => document.getElementById(id);

function getHeaders() {
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${idToken}`
  };
  const totp = el("totpInput").value.trim();
  if (totp) headers["X-Admin-2FA"] = totp;
  return headers;
}

function apiBase() {
  return el("backendUrlInput").value.trim().replace(/\/+$/, "");
}

async function apiJson(path, options = {}) {
  const res = await fetch(`${apiBase()}${path}`, {
    ...options,
    headers: { ...getHeaders(), ...(options.headers || {}) }
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return data;
}

function clearAndPopulateTable(tbody, rows, buildCells) {
  tbody.textContent = "";
  rows.forEach((row) => {
    const tr = document.createElement("tr");
    buildCells(row).forEach((cellNode) => {
      const td = document.createElement("td");
      td.appendChild(cellNode);
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
}

function textNode(value) {
  const span = document.createElement("span");
  span.textContent = value == null ? "" : String(value);
  return span;
}

async function loadStats() {
  const stats = await apiJson("/api/admin/stats");
  el("statUsers").textContent = String(stats.totalUsers ?? 0);
  el("statChats").textContent = String(stats.totalChats ?? 0);
  el("statCalls").textContent = String(stats.totalCalls ?? 0);
  el("statReports").textContent = String(stats.openReports ?? 0);
  el("statCommunities").textContent = String(stats.totalCommunities ?? 0);
}

async function loadUsers(query = "") {
  const data = await apiJson(`/api/admin/users?q=${encodeURIComponent(query)}`);
  clearAndPopulateTable(el("usersTableBody"), data.users || [], (u) => {
    const pill = document.createElement("span");
    pill.className = `status-pill ${u.suspended ? "open" : "ok"}`;
    pill.textContent = u.suspended ? "Suspended" : "Active";

    const btn = document.createElement("button");
    btn.className = u.suspended ? "secondary" : "danger";
    btn.textContent = u.suspended ? "Restore" : "Suspend";
    btn.addEventListener("click", async () => {
      await apiJson(`/api/admin/users/${encodeURIComponent(u.uid)}/suspend`, {
        method: "POST",
        body: JSON.stringify({ suspended: !u.suspended, reason: "Admin console action" })
      });
      await Promise.all([loadUsers(el("userSearchInput").value.trim()), loadLogs()]);
    });

    return [
      textNode(u.displayName || "-"),
      textNode(`@${u.smileId || "-"}`),
      textNode(u.uid || "-"),
      pill,
      btn
    ];
  });
}

async function loadReports() {
  const data = await apiJson("/api/admin/reports");
  clearAndPopulateTable(el("reportsTableBody"), data.reports || [], (r) => {
    const statusStr = String(r.status || "OPEN").toUpperCase();
    const pill = document.createElement("span");
    pill.className = `status-pill ${statusStr === "OPEN" ? "open" : "ok"}`;
    pill.textContent = statusStr;

    const resolveBtn = document.createElement("button");
    resolveBtn.textContent = statusStr === "OPEN" ? "Mark Resolved" : "Resolved";
    resolveBtn.disabled = statusStr !== "OPEN";
    resolveBtn.addEventListener("click", async () => {
      await apiJson(`/api/admin/reports/${encodeURIComponent(r.id)}/resolve`, {
        method: "POST",
        body: JSON.stringify({ status: "RESOLVED", actionTaken: "Reviewed by admin" })
      });
      await Promise.all([loadReports(), loadStats(), loadLogs()]);
    });

    return [
      textNode(r.category || "abuse"),
      textNode(r.reportedUid || "-"),
      textNode(r.reason || "-"),
      pill,
      resolveBtn
    ];
  });
}

async function loadLogs() {
  const data = await apiJson("/api/admin/audit-logs");
  clearAndPopulateTable(el("logsTableBody"), data.logs || [], (l) => [
    textNode(l.action || "-"),
    textNode(l.adminUid || "-"),
    textNode(l.targetId || "-"),
    textNode(JSON.stringify(l.metadata || {}))
  ]);
}

async function refreshAll() {
  await Promise.all([loadStats(), loadUsers(), loadReports(), loadLogs()]);
}

el("googleAdminSignInBtn").addEventListener("click", async () => {
  el("authError").textContent = "";
  const apiKey = el("firebaseApiKeyInput").value.trim();
  const projectId = el("firebaseProjectIdInput").value.trim();
  if (!apiKey || !projectId) {
    el("authError").textContent = "Enter your Firebase Web API Key and Project ID from Firebase Console.";
    return;
  }

  try {
    const app = initializeApp({
      apiKey,
      authDomain: `${projectId}.firebaseapp.com`,
      projectId
    });
    firebaseAuth = getAuth(app);
    const cred = await signInWithPopup(firebaseAuth, new GoogleAuthProvider());
    idToken = await cred.user.getIdToken(true);

    // Verify server RBAC + 2FA before showing dashboard
    await refreshAll();

    el("adminEmailLabel").textContent = cred.user.email || cred.user.uid;
    el("loginView").classList.add("hidden");
    el("dashboardView").classList.remove("hidden");
    el("adminSessionBar").classList.remove("hidden");
  } catch (err) {
    el("authError").textContent = err.message || "Admin authentication failed";
  }
});

el("signOutBtn").addEventListener("click", async () => {
  idToken = "";
  if (firebaseAuth) await signOut(firebaseAuth);
  el("dashboardView").classList.add("hidden");
  el("adminSessionBar").classList.add("hidden");
  el("loginView").classList.remove("hidden");
});

el("searchUsersBtn").addEventListener("click", () => loadUsers(el("userSearchInput").value.trim()));
el("refreshReportsBtn").addEventListener("click", () => loadReports());
el("refreshLogsBtn").addEventListener("click", () => loadLogs());
el("publishAnnounceBtn").addEventListener("click", async () => {
  const title = el("announceTitle").value.trim();
  const body = el("announceBody").value.trim();
  if (!title || !body) return;
  try {
    await apiJson("/api/admin/announcements", {
      method: "POST",
      body: JSON.stringify({ title, body })
    });
    el("announceTitle").value = "";
    el("announceBody").value = "";
    el("announceFeedback").textContent = "Published!";
    await loadLogs();
  } catch (err) {
    el("announceFeedback").textContent = err.message;
  }
});
