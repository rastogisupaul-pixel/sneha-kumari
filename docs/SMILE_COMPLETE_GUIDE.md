# SMILE — Complete Engineering, Deployment & Operations Guide

**Tagline:** बात करें, मुस्कुराएं!  
**Android Package / Application ID:** `com.aistudio.smilechat.kxmpzq`  
**Theme:** Yellow (`#FFD600`) & White (`#FFFFFF`), Dark Charcoal Text (`#111111`), Accent Green (`#22C55E`)

---

## 1. Architecture Overview

SMILE is organized into three separated tiers:
1. **Android Client (`/app`)**: Jetpack Compose + Material 3 app using MVVM (`SessionViewModel`, `HomeViewModel`, `ChatDetailViewModel`), ECDH (`secp256r1`) + HKDF-SHA256 + AES-256-GCM end-to-end encryption (`E2eeCryptoManager`), Credential Manager Google Sign-In + India `+91` profile verification, real-time Firestore listeners, CameraX preview for video calls, and `MediaRecorder`/`MediaPlayer` for voice notes.
2. **Secure Backend API (`/backend`)**: Node.js + Express + `firebase-admin` server providing:
   - Ephemeral Coturn TURN/STUN HMAC credentials (`GET /api/turn-credentials`)
   - Verified participant FCM push notification relay (`POST /api/fcm/notify`)
   - Strict RBAC + 2FA TOTP Admin Panel endpoints (`/api/admin/*`)
   - Rate limiting, Helmet security headers, and strict origin allowlist CORS
3. **Private Web Admin Console (`/admin-dashboard`)**: Completely isolated from the Android user APK. Requires a pre-provisioned `admins/{uid}` Firestore document (or `BOOTSTRAP_ADMIN_EMAILS`), Google Sign-In, and 2FA TOTP code (`X-Admin-2FA`).

---

## 2. Firebase Configuration & Production Upgrade Steps

### Provisioned Database
- **Database ID:** `ai-studio-android-smile-b4208da4-0868-42d4-aa45-6439b6cf9b43`
- **Firestore Security Rules:** `/firestore.rules` (enforces ownership on `users/{userId}`, directory-safe discovery on `userProfiles`, hash-only lookups on `phoneLookup` and `smileIdLookup` with `allow list: if false`, and denormalized `memberUids` checks on chat messages).

### Enabling Additional Production Firebase Services
In the Firebase Console for project `gen-lang-client-0079366378`:
1. **Phone Authentication (OTP):** Enable *Phone* provider under *Authentication -> Sign-in method* and configure Android Play Integrity / SHA-256 fingerprints if adding SMS OTP verification alongside Google Sign-In.
2. **Firebase Cloud Storage (Large Video/Document Media):** Enable Cloud Storage to offload multi-megabyte videos/documents beyond Firestore's document size limit, and bind Storage security rules to chat `memberUids`.
3. **Firebase Cloud Messaging (FCM):** Download server credentials to run `/backend/server.js` so background message & incoming call pushes wake closed devices via high-priority FCM data payloads.

---

## 3. WebRTC & Coturn STUN/TURN Server Setup

To guarantee reliable 1-to-1 and group voice/video calls behind symmetric NATs and mobile carrier firewalls (LTE/5G), deploy **Coturn** on a dedicated Linux VM (Ubuntu 24.04 LTS) with UDP/TCP ports `3478` and TLS `5349` open:

```ini
# /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349
fingerprint
use-auth-secret
static-auth-secret=<SAME_VALUE_AS_TURN_SHARED_SECRET_ENV>
realm=turn.smilechat.example
cert=/etc/letsencrypt/live/turn.smilechat.example/fullchain.pem
pkey=/etc/letsencrypt/live/turn.smilechat.example/privkey.pem
no-loopback-peers
no-multicast-peers
```

The backend endpoint `GET /api/turn-credentials` generates 1-hour HMAC-SHA1 credentials so your static Coturn secret never touches client devices.

---

## 4. Backend & Admin Dashboard Deployment

1. Configure environment secrets (never commit `.env` files):
   - Copy `/backend/.env.example` to your hosting environment secrets (`GOOGLE_APPLICATION_CREDENTIALS`, `TURN_SHARED_SECRET`, `ADMIN_ALLOWED_ORIGINS`, `ADMIN_TOTP_SECRET`).
2. Start backend:
   ```bash
   cd backend
   npm install --omit=dev
   npm start
   ```
3. Host `/admin-dashboard` on an internal/VPN-restricted static host and add its exact origin to `ADMIN_ALLOWED_ORIGINS`.
4. Provision the first admin by adding a document in Firestore `admins/{adminUid}` with `{ "active": true, "role": "super_admin", "totpSecret": "<BASE32_SECRET>" }`.

---

## 5. Google Play Store Publishing & Policy Checklist

- **Permissions Audit:**
  - Uses Android Photo Picker (`PickVisualMedia`) and Storage Access Framework (`OpenDocument`) — **no** `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, or `READ_MEDIA_VIDEO` permissions declared.
  - Runtime permissions (`CAMERA`, `RECORD_AUDIO`, `ACCESS_COARSE_LOCATION`, `READ_CONTACTS`, `POST_NOTIFICATIONS`) are requested just-in-time with clear rationale and consent dialogs.
- **Release Signing:**
  - Generate an upload keystore securely offline and supply `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` via CI/AI Studio Secrets.
  - Build Android App Bundle: `gradle :app:bundleRelease`.
- **Store Listing Requirements:**
  - App Title: `SMILE` (under 30 chars, no buzzwords).
  - Target API Level: SDK 36 (`targetSdk = 36`, `minSdk = 24`).
  - Complete the Data Safety form declaring encrypted in-transit/at-rest messages, phone number for discovery, and account deletion support (built directly into Settings -> Delete Account).

---

## 6. Privacy Policy & Terms of Service Summary

- **End-to-End Encryption:** Private chat messages are encrypted on-device using ECDH (`secp256r1`) key agreement and AES-256-GCM before transmission.
- **Phone Number Hashing:** Phone discovery uses SHA-256 hashes (`phoneLookup/{hash}`) so unauthenticated users cannot scrape registered phone numbers.
- **User Controls:** Users can block/report accounts, enable 4-digit PIN + biometric app lock, toggle Last Seen/Photo/About/Read Receipt visibility, set disappearing message timers, or permanently delete their account and lookup records inside Settings.
