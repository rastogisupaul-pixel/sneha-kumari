const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all SMILE collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("userProfiles").where("publicDirectory", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("memberUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("statuses").where("allowedUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("calls").where("participantUids", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("communities").where("memberUids", "array-contains", ALICE_UID).get());
});

test("Private user account isolates PII and rejects cross-user access or list queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "+919876543210",
      language: "en",
      darkMode: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ phoneNumber: "+910000000000", updatedAt: now }));
  // List on users must fail
  await assertFails(aliceDb.collection("users").get());
  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true, updatedAt: now })
  );
});

test("UserProfile and exact lookups enforce ownership and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("userProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      smileId: "aarav_smile",
      displayName: "Aarav Sharma",
      about: "Available on SMILE",
      phoneHash: "hash_919876543210",
      ecdhPublicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEtestkey",
      online: true,
      lastSeen: now,
      photoVisibility: "EVERYONE",
      aboutVisibility: "EVERYONE",
      lastSeenVisibility: "EVERYONE",
      readReceiptsEnabled: true,
      publicDirectory: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered directory query succeeds; unfiltered list fails
  await assertSucceeds(aliceDb.collection("userProfiles").where("publicDirectory", "==", true).get());
  await assertFails(bobDb.collection("userProfiles").get());

  // Bob cannot update Alice's profile
  await assertFails(bobDb.collection("userProfiles").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: now }));

  // Phone & Smile ID lookup allow get, deny list
  await assertSucceeds(
    aliceDb.collection("phoneLookup").doc("hash_919876543210").set({
      userId: ALICE_UID,
      smileId: "aarav_smile",
      displayName: "Aarav Sharma",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("phoneLookup").doc("hash_919876543210").get());
  await assertFails(bobDb.collection("phoneLookup").get());
});

test("Chats and Messages enforce memberUids isolation, E2EE payloads, and action updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      chatType: "DIRECT",
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      coAdminUids: [],
      title: "",
      description: "",
      inviteCode: "",
      onlyAdminsCanSend: false,
      disappearingSeconds: 0,
      communityId: "",
      lastMessageText: "",
      lastMessageSenderId: "",
      typingUids: [],
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered chat query succeeds for Bob; Charlie cannot read or list
  await assertSucceeds(bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("chats").doc("chat_ab").get());
  await assertFails(charlieDb.collection("chats").get());

  // Alice sends encrypted message
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "Aarav",
      memberUids: [ALICE_UID, BOB_UID],
      messageType: "TEXT",
      ciphertext: "base64ciphertext==",
      iv: "base64iv==",
      plainFallback: "",
      forwarded: false,
      edited: false,
      deletedForEveryone: false,
      deletedForUids: [],
      reactions: {},
      starredByUids: [],
      readByUids: [ALICE_UID],
      deliveredToUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob queries messages with memberUids filter
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages")
      .where("memberUids", "array-contains", BOB_UID).get()
  );
  // Charlie cannot read Alice/Bob's message directly or run an unfiltered list query
  await assertFails(
    charlieDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("chats").doc("chat_ab").collection("messages").get()
  );

  // Bob can react/mark read, but CANNOT edit Alice's ciphertext
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").update({
      reactions: { [BOB_UID]: "😊" },
      readByUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").update({
      ciphertext: "tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Statuses, Calls, Communities, and Reports enforce participant/owner rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  const tomorrow = new Date(Date.now() + 86400000);

  // Status
  await assertSucceeds(
    aliceDb.collection("statuses").doc("st_1").set({
      userId: ALICE_UID,
      userName: "Aarav",
      statusType: "TEXT",
      textContent: "Good morning SMILE!",
      bgColorHex: "#FFD600",
      caption: "",
      privacy: "CONTACTS",
      allowedUids: [ALICE_UID, BOB_UID],
      mutedByUids: [],
      expiresAt: tomorrow,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("statuses").where("allowedUids", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("statuses").doc("st_1").get());

  // Call signaling
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Aarav",
      calleeIds: [BOB_UID],
      participantUids: [ALICE_UID, BOB_UID],
      chatId: "chat_ab",
      callType: "VIDEO",
      status: "RINGING",
      sdpOffer: "v=0...",
      durationSeconds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_1").update({
      status: "ACCEPTED",
      sdpAnswer: "v=0...",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(charlieDb.collection("calls").doc("call_1").get());

  // Community
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_1").set({
      name: "SMILE Builders",
      description: "Community for creators",
      adminUids: [ALICE_UID],
      memberUids: [ALICE_UID, BOB_UID],
      linkedChatIds: [],
      announcementChatId: "chat_comm_1",
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("communities").where("memberUids", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("communities").doc("comm_1").get());

  // Report
  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      targetUserId: CHARLIE_UID,
      reason: "Spam",
      details: "Unsolicited links",
      reportType: "USER_ABUSE",
      status: "OPEN",
      createdAt: now,
    })
  );
  await assertFails(bobDb.collection("reports").doc("rep_1").get());
});
