const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const OWNER_UID = "owner_sbb";
const OWNER_EMAIL = "sbb521523@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function baseProfile(uid, username, displayName, extra = {}) {
  return {
    userId: uid,
    username,
    displayName,
    avatarUrl: "",
    bannerUrl: "",
    bio: "",
    favoriteGames: "",
    postsCount: 0,
    videosCount: 0,
    reelsCount: 0,
    followersCount: 0,
    followingCount: 0,
    friendsCount: 0,
    isVerified: false,
    isOwner: false,
    isOnline: true,
    createdAt: new Date(),
    updatedAt: new Date(),
    ...extra,
  };
}

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

test("Unauthenticated user: cannot read private user info or public profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice Gamer"))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Verification security: regular user and unverified email spoof cannot claim isVerified or isOwner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      baseProfile(ALICE_UID, "alice_prime", "Alice Gamer", { isVerified: true })
    )
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(
      baseProfile(ALICE_UID, "alice_prime", "Alice Gamer", { isOwner: true })
    )
  );

  // Unverified spoof of owner email must fail
  const spoofDb = testEnv.authenticatedContext("spoof_uid", { email: OWNER_EMAIL, email_verified: false }).firestore();
  await assertFails(
    spoofDb.collection("users").doc("spoof_uid").set(
      baseProfile("spoof_uid", "spoof_owner", "Spoof", { isOwner: true, isVerified: true })
    )
  );

  // Regular user cannot self-verify on update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice Gamer"))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ isVerified: true })
  );
});

test("Official Owner (verified sbb521523@gmail.com): can claim owner/verified and verify another creator", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(
    ownerDb.collection("users").doc(OWNER_UID).set(
      baseProfile(OWNER_UID, "prime_owner", "Official Owner", { isOwner: true, isVerified: true })
    )
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice Gamer"))
  );
  await assertSucceeds(
    ownerDb.collection("users").doc(ALICE_UID).update({ isVerified: true, updatedAt: new Date() })
  );
});

test("Cross-user access: Bob cannot edit Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice Gamer"));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked by Bob",
    })
  );
});

test("Follow system: atomic batch follow updates followersCount; forged count or self-follow fails", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice"));
    await adminDb.collection("users").doc(BOB_UID).set(baseProfile(BOB_UID, "bob_prime", "Bob"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Forged followersCount without follow doc fails
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ followersCount: 99, updatedAt: new Date() })
  );

  // Self-follow fails
  await assertFails(
    aliceDb.collection("follows").doc(`${ALICE_UID}_${ALICE_UID}`).set({
      followerId: ALICE_UID,
      followingId: ALICE_UID,
      createdAt: new Date(),
    })
  );

  // Valid atomic batch follow succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("follows").doc(`${ALICE_UID}_${BOB_UID}`), {
    followerId: ALICE_UID,
    followingId: BOB_UID,
    createdAt: new Date(),
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    followingCount: 1,
    updatedAt: new Date(),
  });
  batch.update(aliceDb.collection("users").doc(BOB_UID), {
    followersCount: 1,
    updatedAt: new Date(),
  });
  await assertSucceeds(batch.commit());
});

test("Friend request lifecycle & friend-gated conversations", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice"));
    await adminDb.collection("users").doc(BOB_UID).set(baseProfile(BOB_UID, "bob_prime", "Bob"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const reqId = `${ALICE_UID}_${BOB_UID}`;
  const convId = `conv_${ALICE_UID}_${BOB_UID}`;

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();

  // Signed-in users can check existence of non-existent friendRequests/friendships/conversations before creation
  await assertSucceeds(aliceDb.collection("friendships").doc(`${ALICE_UID}_${BOB_UID}`).get());
  await assertSucceeds(aliceDb.collection("friendRequests").doc(reqId).get());
  await assertSucceeds(aliceDb.collection("conversations").doc(convId).get());

  // Cannot create DM conversation before friendship
  await assertFails(
    aliceDb.collection("conversations").doc(convId).set({
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "",
      lastSenderId: "",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Send friend request
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc(reqId).set({
      senderId: ALICE_UID,
      senderUsername: "alice_prime",
      senderAvatarUrl: "",
      receiverId: BOB_UID,
      status: "pending",
      createdAt: new Date(),
    })
  );

  // Sender cancels pending request, then re-sends
  await assertSucceeds(aliceDb.collection("friendRequests").doc(reqId).update({ status: "cancelled", updatedAt: new Date() }));
  await assertSucceeds(aliceDb.collection("friendRequests").doc(reqId).update({ status: "pending", updatedAt: new Date() }));

  // Bob accepts & creates friendship + updates friendsCount atomically
  const acceptBatch = bobDb.batch();
  acceptBatch.update(bobDb.collection("friendRequests").doc(reqId), { status: "accepted", updatedAt: new Date() });
  acceptBatch.set(bobDb.collection("friendships").doc(`${ALICE_UID}_${BOB_UID}`), {
    user1: ALICE_UID,
    user2: BOB_UID,
    users: [ALICE_UID, BOB_UID],
    createdAt: new Date(),
  });
  acceptBatch.update(bobDb.collection("users").doc(BOB_UID), { friendsCount: 1, updatedAt: new Date() });
  acceptBatch.update(bobDb.collection("users").doc(ALICE_UID), { friendsCount: 1, updatedAt: new Date() });
  await assertSucceeds(acceptBatch.commit());

  // Now conversation creation succeeds
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participants: [ALICE_UID, BOB_UID],
      lastMessage: "",
      lastSenderId: "",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Third party cannot read existing friendRequest, friendship, or conversation
  await assertFails(ownerDb.collection("friendRequests").doc(reqId).get());
  await assertFails(ownerDb.collection("friendships").doc(`${ALICE_UID}_${BOB_UID}`).get());
  await assertFails(ownerDb.collection("conversations").doc(convId).get());
});

test("Posts, Reels & ChannelVideos: owner-only create & delete; non-owner delete rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_prime",
      content: "GG in the tournament today!",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("reels").doc("reel_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_prime",
      videoUrl: "https://example.com/clip.mp4",
      storagePath: `reels/${ALICE_UID}/clip.mp4`,
      caption: "Clutch ace!",
      tags: "#gaming",
      likesCount: 0,
      commentsCount: 0,
      viewsCount: 0,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("channelVideos").doc("vid_1").set({
      channelId: ALICE_UID,
      ownerId: ALICE_UID,
      authorUsername: "alice_prime",
      title: "Ranked Guide",
      videoUrl: "https://example.com/guide.mp4",
      status: "published",
      viewsCount: 0,
      createdAt: new Date(),
    })
  );

  // Bob cannot delete Alice's content
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertFails(bobDb.collection("reels").doc("reel_1").delete());
  await assertFails(bobDb.collection("channelVideos").doc("vid_1").delete());

  // Separate channel creation is disabled
  await assertFails(
    aliceDb.collection("channels").doc("ch_1").set({
      ownerId: ALICE_UID,
      name: "My Second Channel",
    })
  );

  // Alice can delete her own content
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").delete());
  await assertSucceeds(aliceDb.collection("reels").doc("reel_1").delete());
  await assertSucceeds(aliceDb.collection("channelVideos").doc("vid_1").delete());
});

test("Notifications: sender creates unread notification for receiver; only receiver reads and marks read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Signed-in sender can check existence of a non-existent dedup notification before creation
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_1").get());

  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_1").set({
      userId: BOB_UID,
      senderId: ALICE_UID,
      senderName: "alice_prime",
      senderAvatarUrl: "",
      title: "New Follower",
      body: "@alice_prime started following you",
      type: "follow",
      targetId: ALICE_UID,
      isRead: false,
      createdAt: new Date(),
    })
  );

  // Self-notification rejected
  await assertFails(
    aliceDb.collection("notifications").doc("notif_self").set({
      userId: ALICE_UID,
      senderId: ALICE_UID,
      title: "Self",
      body: "Self",
      type: "general",
      isRead: false,
      createdAt: new Date(),
    })
  );

  // Alice cannot read Bob's notification
  await assertFails(aliceDb.collection("notifications").doc("notif_1").get());

  // Bob can read and mark read, then delete
  await assertSucceeds(bobDb.collection("notifications").doc("notif_1").get());
  await assertSucceeds(bobDb.collection("notifications").doc("notif_1").update({ isRead: true }));
  await assertSucceeds(bobDb.collection("notifications").doc("notif_1").delete());
});

test("User blocks: owner can block/list/unblock; self-block, update, and cross-user access fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const blockId = `${ALICE_UID}_${BOB_UID}`;

  // Signed-in user can check non-existent block document
  await assertSucceeds(aliceDb.collection("blocks").doc(blockId).get());

  // Self-block fails
  await assertFails(
    aliceDb.collection("blocks").doc(`${ALICE_UID}_${ALICE_UID}`).set({
      blockerId: ALICE_UID,
      blockedId: ALICE_UID,
      createdAt: new Date(),
    })
  );

  // Mismatched document ID fails
  await assertFails(
    aliceDb.collection("blocks").doc("wrong_id").set({
      blockerId: ALICE_UID,
      blockedId: BOB_UID,
      createdAt: new Date(),
    })
  );

  // Valid block succeeds and owner can query own blocks
  await assertSucceeds(
    aliceDb.collection("blocks").doc(blockId).set({
      blockerId: ALICE_UID,
      blockedId: BOB_UID,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("blocks").where("blockerId", "==", ALICE_UID).get());

  // Block documents are immutable
  await assertFails(aliceDb.collection("blocks").doc(blockId).update({ blockedId: OWNER_UID }));

  // Bob cannot read or delete Alice's block
  await assertFails(bobDb.collection("blocks").doc(blockId).get());
  await assertFails(bobDb.collection("blocks").where("blockerId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("blocks").doc(blockId).delete());

  // Alice can unblock (delete)
  await assertSucceeds(aliceDb.collection("blocks").doc(blockId).delete());
});

test("Tournaments: only verified creators or Official Owner can create; Official Owner can delete for moderation", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice", { isVerified: false }));
    await adminDb.collection("users").doc(BOB_UID).set(baseProfile(BOB_UID, "bob_verified", "Bob Pro", { isVerified: true }));
    await adminDb.collection("users").doc(OWNER_UID).set(baseProfile(OWNER_UID, "prime_owner", "Official Owner", { isOwner: true, isVerified: true }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();

  const sampleTournament = (organizerId, organizerName) => ({
    organizerId,
    organizerName,
    title: "Prime Winter Cup",
    game: "BGMI",
    description: "Official qualifiers",
    prizePool: "TBD",
    teamSize: 4,
    maxTeams: 16,
    registeredTeamsCount: 0,
    startDate: "20 Nov 2026",
    status: "upcoming",
    rules: "Fair play",
    createdAt: new Date(),
    updatedAt: new Date(),
  });

  // Regular unverified user Alice cannot create a tournament
  await assertFails(
    aliceDb.collection("tournaments").doc("tour_unauth").set(sampleTournament(ALICE_UID, "Alice"))
  );

  // Verified user Bob can create a tournament
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_bob").set(sampleTournament(BOB_UID, "Bob Pro"))
  );

  // Unverified non-organizer Alice cannot delete Bob's tournament
  await assertFails(aliceDb.collection("tournaments").doc("tour_bob").delete());

  // Official Owner can delete Bob's tournament for moderation
  await assertSucceeds(ownerDb.collection("tournaments").doc("tour_bob").delete());

  // Official Owner can also create a tournament
  await assertSucceeds(
    ownerDb.collection("tournaments").doc("tour_owner").set(sampleTournament(OWNER_UID, "Official Owner"))
  );
  await assertSucceeds(ownerDb.collection("tournaments").doc("tour_owner").delete());
});

test("Communities: atomic batch create with owner member + general channel succeeds; Official Owner can delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();

  const commRef = aliceDb.collection("communities").doc("comm_1");
  const batch = aliceDb.batch();
  batch.set(commRef, {
    ownerId: ALICE_UID,
    name: "Valorant Scrims",
    description: "Daily 5v5 customs",
    iconUrl: "",
    memberCount: 1,
    isPublic: true,
    rules: "Respect everyone",
    createdAt: new Date(),
    updatedAt: new Date(),
  });
  batch.set(commRef.collection("members").doc(ALICE_UID), {
    communityId: "comm_1",
    userId: ALICE_UID,
    username: "alice_prime",
    avatarUrl: "",
    role: "owner",
    joinedAt: new Date(),
  });
  batch.set(commRef.collection("channels").doc("general"), {
    communityId: "comm_1",
    name: "general-chat",
    topic: "General discussions",
    type: "gaming",
    createdAt: new Date(),
  });
  await assertSucceeds(batch.commit());

  // Non-owner Bob cannot delete Alice's community
  await assertFails(bobDb.collection("communities").doc("comm_1").delete());

  // Official Owner can delete Alice's community
  await assertSucceeds(ownerDb.collection("communities").doc("comm_1").delete());
});

test("User gaming identity: owner can save handles, favoriteTitles, rankHistory, socialLinks; cross-user & oversized rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice Gamer"))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteTitles: ["Valorant", "BGMI", "Minecraft"],
      favoriteGames: "Valorant, BGMI, Minecraft",
      gameHandles: [{ platform: "Valorant", handle: "Alice#PRIME" }],
      rankHistory: [{ game: "Valorant", season: "Ep 9 Act 2", rank: "Ascendant II", peakRank: "Immortal I", notes: "Controller" }],
      socialLinks: [{ platform: "YouTube", handleOrUrl: "https://youtube.com/@aliceprime" }],
      updatedAt: new Date(),
    })
  );

  // Oversized favoriteTitles (> 20) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteTitles: Array.from({ length: 21 }, (_, i) => `Game ${i}`),
      updatedAt: new Date(),
    })
  );

  // Bob cannot overwrite Alice's gaming identity
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      gameHandles: [{ platform: "Steam", handle: "Hacked" }],
      updatedAt: new Date(),
    })
  );
});

test("Fake communities and tournaments: confirmed demo names cannot be created, legacy system_demo docs can be purged", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc(ALICE_UID).set(baseProfile(ALICE_UID, "alice_prime", "Alice", { isVerified: false }));
    await adminDb.collection("users").doc(OWNER_UID).set(baseProfile(OWNER_UID, "prime_owner", "Official Owner", { isOwner: true, isVerified: true }));
    await adminDb.collection("communities").doc("legacy_fake_comm").set({
      ownerId: "system_demo",
      name: "Prime Esports Elite",
      description: "Seeded demo community",
      memberCount: 99,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    await adminDb.collection("tournaments").doc("legacy_fake_tourn").set({
      organizerId: "system_demo",
      organizerName: "System",
      title: "BGMI Masters Series Pro Cup",
      game: "BGMI",
      prizePool: "100000",
      status: "upcoming",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: OWNER_EMAIL, email_verified: true }).firestore();

  // Confirmed fake community name rejected by rules
  await assertFails(
    aliceDb.collection("communities").doc("fake_comm_attempt").set({
      ownerId: ALICE_UID,
      name: "Prime Esports Elite",
      description: "Fake clone",
      memberCount: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Confirmed fake tournament title rejected even for Official Owner
  await assertFails(
    ownerDb.collection("tournaments").doc("fake_tourn_attempt").set({
      organizerId: OWNER_UID,
      organizerName: "Official Owner",
      title: "BGMI Masters Series Pro Cup",
      game: "BGMI",
      prizePool: "50000",
      status: "upcoming",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Signed-in user can delete confirmed legacy system_demo fake community & tournament
  await assertSucceeds(aliceDb.collection("communities").doc("legacy_fake_comm").delete());
  await assertSucceeds(aliceDb.collection("tournaments").doc("legacy_fake_tourn").delete());
});



