// Client-side session cache, kept in sync with the server-side session.
// The cache is only used for fast UI rendering (sidebar, greeting) — every
// actual permission check happens server-side via the session cookie +
// Spring Security, so a tampered sessionStorage value can't grant access.
const SESSION_KEY = "carehub_user";

function saveSession(user) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user));
}

function currentUser() {
  const raw = sessionStorage.getItem(SESSION_KEY);
  return raw ? JSON.parse(raw) : null;
}

function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

// Fast local check to avoid a flash of protected content, then verifies the
// real session with the server in the background (handles expired sessions,
// deactivated accounts, etc.)
function requireAuth() {
  const user = currentUser();
  if (!user) {
    window.location.href = "login.html";
    return null;
  }
  Api.get("/auth/me")
    .then(fresh => saveSession(fresh))
    .catch(() => {
      clearSession();
      window.location.href = "login.html";
    });
  return user;
}

async function logout() {
  try { await Api.post("/auth/logout"); } catch (e) { /* ignore */ }
  clearSession();
  window.location.href = "login.html";
}
