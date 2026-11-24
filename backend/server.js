require("dotenv").config();
const crypto = require("crypto");
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const admin = require("firebase-admin");
const { getFirestore } = require("firebase-admin/firestore");
const { authenticator } = require("otplib");

const app = express();
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "64kb" }));

const allowedOrigins = (process.env.ADMIN_ALLOWED_ORIGINS || "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.includes(origin)) {
        return callback(null, true);
      }
      return callback(new Error("Origin not allowed by CORS"));
    },
    credentials: true
  })
);

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: process.env.GOOGLE_CLOUD_PROJECT || undefined
  });
}

const databaseId =
  process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-smile-b4208da4-0868-42d4-aa45-6439b6cf9b43";
const db = getFirestore(admin.app(), databaseId);

const apiLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  standardHeaders: true,
  legacyHeaders: false
});

const strictAdminLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 120,
  standardHeaders: true,
  legacyHeaders: false
});

app.use("/api/", apiLimiter);

function sanitizeString(value, maxLen = 200) {
  if (typeof value !== "string") return "";
  return value.trim().slice(0, maxLen);
}

async function requireFirebaseUser(req, res, next) {
  try {
    const authHeader = req.headers.authorization || "";
    const match = authHeader.match(/^Bearer\s+(.+)$/i);
    if (!match) {
      return res.status(401).json({ error: "Missing Bearer token" });
    }
    const decoded = await admin.auth().verifyIdToken(match[1], true);
    req.user = decoded;
    return next();
  } catch (_err) {
    return res.status(401).json({ error: "Invalid or expired Firebase ID token" });
  }
}

async function requireAdmin(req, res, next) {
  await requireFirebaseUser(req, res, async () => {
    try {
      const uid = req.user.uid;
      const email = (req.user.email || "").toLowerCase();
      const adminDoc = await db.collection("admins").doc(uid).get();
      const bootstrapEmails = (process.env.BOOTSTRAP_ADMIN_EMAILS || "")
        .split(",")
        .map((e) => e.trim().toLowerCase())
        .filter(Boolean);

      const isDbAdmin = adminDoc.exists && adminDoc.data()?.active === true;
      const isBootstrapAdmin = email && bootstrapEmails.includes(email);

      if (!isDbAdmin && !isBootstrapAdmin) {
        return res.status(403).json({ error: "Admin RBAC privileges required" });
      }

      req.adminRole = adminDoc.exists ? adminDoc.data().role || "moderator" : "super_admin";

      const totpSecret = (adminDoc.exists && adminDoc.data()?.totpSecret) || process.env.ADMIN_TOTP_SECRET || "";
      if (totpSecret) {
        const otpCode = sanitizeString(req.headers["x-admin-2fa"], 12);
        if (!otpCode || !authenticator.check(otpCode, totpSecret)) {
          return res.status(403).json({ error: "Valid 2FA TOTP code required in X-Admin-2FA header" });
        }
      }

      return next();
    } catch (_err) {
      return res.status(500).json({ error: "Admin authorization check failed" });
    }
  });
}

async function writeAuditLog(adminUid, action, targetId, metadata = {}) {
  await db.collection("adminAuditLogs").add({
    adminUid,
    action,
    targetId,
    metadata,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
}

app.get("/health", (_req, res) => {
  res.json({ status: "ok", service: "smile-backend" });
});

/**
 * Generates ephemeral Coturn REST API TURN/STUN credentials for WebRTC voice/video calls.
 * Never exposes static TURN secrets to clients.
 */
app.get("/api/turn-credentials", requireFirebaseUser, (req, res) => {
  const secret = process.env.TURN_SHARED_SECRET;
  const host = process.env.TURN_HOST || "turn.smilechat.example";
  if (!secret) {
    return res.status(503).json({
      error: "TURN_SHARED_SECRET not configured on server. Configure Coturn shared secret in environment."
    });
  }
  const ttlSeconds = 3600;
  const expiry = Math.floor(Date.now() / 1000) + ttlSeconds;
  const username = `${expiry}:${req.user.uid}`;
  const credential = crypto.createHmac("sha1", secret).update(username).digest("base64");

  return res.json({
    ttl: ttlSeconds,
    iceServers: [
      { urls: [`stun:${host}:3478`] },
      {
        urls: [`turn:${host}:3478?transport=udp`, `turns:${host}:5349?transport=tcp`],
        username,
        credential
      }
    ]
  });
});

/**
 * Relays high-priority FCM push notifications for incoming calls and unread messages.
 * Only sends to verified chat/call participants.
 */
app.post("/api/fcm/notify", requireFirebaseUser, async (req, res) => {
  try {
    const targetUid = sanitizeString(req.body?.targetUid, 128);
    const kind = sanitizeString(req.body?.kind, 32); // "message" | "call"
    const referenceId = sanitizeString(req.body?.referenceId, 128);

    if (!targetUid || !["message", "call"].includes(kind) || !referenceId) {
      return res.status(400).json({ error: "Invalid notification payload" });
    }

    if (kind === "message") {
      const chatSnap = await db.collection("chats").doc(referenceId).get();
      if (!chatSnap.exists || !(chatSnap.data()?.memberUids || []).includes(req.user.uid) || !(chatSnap.data()?.memberUids || []).includes(targetUid)) {
        return res.status(403).json({ error: "Caller is not a participant of this chat" });
      }
    } else if (kind === "call") {
      const callSnap = await db.collection("calls").doc(referenceId).get();
      if (!callSnap.exists || !(callSnap.data()?.participantUids || []).includes(req.user.uid) || !(callSnap.data()?.participantUids || []).includes(targetUid)) {
        return res.status(403).json({ error: "Caller is not a participant of this call" });
      }
    }

    const targetPrivate = await db.collection("users").doc(targetUid).get();
    const fcmToken = targetPrivate.data()?.fcmToken;
    if (!fcmToken) {
      return res.json({ delivered: false, reason: "No registered FCM token" });
    }

    await admin.messaging().send({
      token: fcmToken,
      android: { priority: "high" },
      data: {
        type: kind,
        referenceId,
        senderUid: req.user.uid
      }
    });

    return res.json({ delivered: true });
  } catch (_err) {
    return res.status(500).json({ error: "Failed to dispatch FCM notification" });
  }
});

// ==================== ADMIN PANEL RBAC ENDPOINTS ====================

app.get("/api/admin/stats", strictAdminLimiter, requireAdmin, async (_req, res) => {
  try {
    const [usersAgg, chatsAgg, callsAgg, reportsAgg, communitiesAgg] = await Promise.all([
      db.collection("userProfiles").count().get(),
      db.collection("chats").count().get(),
      db.collection("calls").count().get(),
      db.collection("reports").where("status", "==", "OPEN").count().get(),
      db.collection("communities").count().get()
    ]);

    res.json({
      totalUsers: usersAgg.data().count,
      totalChats: chatsAgg.data().count,
      totalCalls: callsAgg.data().count,
      openReports: reportsAgg.data().count,
      totalCommunities: communitiesAgg.data().count
    });
  } catch (_err) {
    res.status(500).json({ error: "Failed to query metrics" });
  }
});

app.get("/api/admin/users", strictAdminLimiter, requireAdmin, async (req, res) => {
  try {
    const q = sanitizeString(req.query.q, 64).toLowerCase();
    const snap = await db.collection("userProfiles").limit(50).get();
    const users = snap.docs
      .map((d) => ({ uid: d.id, ...d.data() }))
      .filter((u) => !q || (u.displayName || "").toLowerCase().includes(q) || (u.smileId || "").toLowerCase().includes(q) || u.uid.includes(q));
    res.json({ users });
  } catch (_err) {
    res.status(500).json({ error: "Failed to fetch users" });
  }
});

app.post("/api/admin/users/:uid/suspend", strictAdminLimiter, requireAdmin, async (req, res) => {
  try {
    const targetUid = sanitizeString(req.params.uid, 128);
    const suspended = Boolean(req.body?.suspended);
    const reason = sanitizeString(req.body?.reason, 250);
    if (!targetUid) return res.status(400).json({ error: "Missing uid" });

    await db.collection("userProfiles").doc(targetUid).set({ suspended }, { merge: true });
    await db.collection("users").doc(targetUid).set({ suspended, suspensionReason: reason }, { merge: true });
    if (suspended) {
      await admin.auth().revokeRefreshTokens(targetUid);
    }
    await writeAuditLog(req.user.uid, suspended ? "SUSPEND_USER" : "UNSUSPEND_USER", targetUid, { reason });
    return res.json({ ok: true, targetUid, suspended });
  } catch (_err) {
    return res.status(500).json({ error: "Failed to update user suspension state" });
  }
});

app.get("/api/admin/reports", strictAdminLimiter, requireAdmin, async (_req, res) => {
  try {
    const snap = await db.collection("reports").orderBy("createdAt", "desc").limit(100).get();
    res.json({ reports: snap.docs.map((d) => ({ id: d.id, ...d.data() })) });
  } catch (_err) {
    res.status(500).json({ error: "Failed to load reports" });
  }
});

app.post("/api/admin/reports/:reportId/resolve", strictAdminLimiter, requireAdmin, async (req, res) => {
  try {
    const reportId = sanitizeString(req.params.reportId, 128);
    const actionTaken = sanitizeString(req.body?.actionTaken, 200) || "reviewed";
    const rawStatus = sanitizeString(req.body?.status, 20).toUpperCase();
    const status = ["REVIEWED", "RESOLVED"].includes(rawStatus) ? rawStatus : "RESOLVED";
    await db.collection("reports").doc(reportId).set(
      {
        status,
        actionTaken,
        reviewedBy: req.user.uid,
        reviewedAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );
    await writeAuditLog(req.user.uid, "RESOLVE_REPORT", reportId, { status, actionTaken });
    res.json({ ok: true });
  } catch (_err) {
    res.status(500).json({ error: "Failed to resolve report" });
  }
});

app.post("/api/admin/announcements", strictAdminLimiter, requireAdmin, async (req, res) => {
  try {
    const title = sanitizeString(req.body?.title, 120);
    const body = sanitizeString(req.body?.body, 1000);
    if (!title || !body) return res.status(400).json({ error: "Title and body required" });

    const docRef = await db.collection("announcements").add({
      title,
      body,
      authorUid: req.user.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    await writeAuditLog(req.user.uid, "PUBLISH_ANNOUNCEMENT", docRef.id, { title });
    return res.json({ ok: true, id: docRef.id });
  } catch (_err) {
    return res.status(500).json({ error: "Failed to publish announcement" });
  }
});

app.get("/api/admin/audit-logs", strictAdminLimiter, requireAdmin, async (_req, res) => {
  try {
    const snap = await db.collection("adminAuditLogs").orderBy("createdAt", "desc").limit(100).get();
    res.json({ logs: snap.docs.map((d) => ({ id: d.id, ...d.data() })) });
  } catch (_err) {
    res.status(500).json({ error: "Failed to load audit logs" });
  }
});

const port = Number(process.env.PORT || 8080);
if (require.main === module) {
  app.listen(port, () => {
    console.log(`SMILE backend listening on port ${port}`);
  });
}

module.exports = { app };
