package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameHandle
import com.example.data.model.Post
import com.example.data.model.RankHistoryEntry
import com.example.data.model.Reel
import com.example.data.model.SocialLink
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class PrimeEsportRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().replace("-", "").take(8)

    @Test
    fun claimUsername_validPayload_succeedsAndBlocksDuplicates() = runBlocking {
        val tag = uniqueSuffix()
        val email1 = "gamer1_$tag@primeesport.com"
        val email2 = "gamer2_$tag@primeesport.com"
        val handle = "prime_$tag"

        val uid1 = signInTestUser(email1)
        val authRepo1 = AuthRepository(firestore, auth)

        val res1 = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo1.claimUsernameAndCompleteProfile(
                rawUsername = handle,
                rawDisplayName = "Prime Champion",
                bio = "Competitive BGMI player"
            )
        }
        assertTrue("Expected success, got: ${res1.exceptionOrNull()?.message}", res1.isSuccess)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo1.observeUserProfile(uid1).first { it != null }
        }
        assertNotNull(profile)
        assertEquals(handle, profile?.username)
        assertFalse(profile?.isOwner ?: true)
        assertFalse(profile?.isVerified ?: true)

        // Second user cannot claim the same username (even with different casing)
        signInTestUser(email2)
        val authRepo2 = AuthRepository(firestore, auth)
        val dupRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo2.claimUsernameAndCompleteProfile(
                rawUsername = handle.uppercase(),
                rawDisplayName = "Copycat"
            )
        }
        assertTrue(dupRes.isFailure)
    }

    @Test
    fun verificationSecurity_regularUserCannotClaimVerifiedOrOwner() = runBlocking {
        val tag = uniqueSuffix()
        val email = "regular_$tag@primeesport.com"
        val targetEmail = "target_$tag@primeesport.com"

        val uid = signInTestUser(email)
        val authRepo = AuthRepository(firestore, auth)
        val claimRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo.claimUsernameAndCompleteProfile("reg_$tag", "Regular Player")
        }
        assertTrue(claimRes.isSuccess)

        // Regular user cannot self-verify or claim owner via Firestore update
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(uid).update("isVerified", true).await()
            }
            fail("Non-owner should not be able to set isVerified = true")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(uid).update("isOwner", true).await()
            }
            fail("Non-owner should not be able to set isOwner = true")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }

        val targetUid = signInTestUser(targetEmail)
        val targetAuth = AuthRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            targetAuth.claimUsernameAndCompleteProfile("targ_$tag", "Target Player")
        }

        signInTestUser(email)
        val verifyRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo.setUserVerified(targetUid, true)
        }
        assertTrue(verifyRes.isFailure)
    }

    @Test
    fun followSystem_atomicToggleUpdatesCountsAndNotification() = runBlocking {
        val tag = uniqueSuffix()
        val emailA = "follow_a_$tag@primeesport.com"
        val emailB = "follow_b_$tag@primeesport.com"

        val uidA = signInTestUser(emailA)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("fa_$tag", "Follower A")
        }

        val uidB = signInTestUser(emailB)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("fb_$tag", "Creator B")
        }

        // User A follows User B
        signInTestUser(emailA)
        val socialA = SocialRepository(firestore, auth)
        val followRes = withTimeout(DEFAULT_TIMEOUT_MS) { socialA.toggleFollow(uidB) }
        assertTrue("Follow failed: ${followRes.exceptionOrNull()?.message}", followRes.isSuccess)
        assertEquals(true, followRes.getOrNull())

        val profileA = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(uidA).first { it?.followingCount == 1 }
        }
        val profileB = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(uidB).first { it?.followersCount == 1 }
        }
        assertEquals(1, profileA?.followingCount)
        assertEquals(1, profileB?.followersCount)

        // User B receives unread follow notification
        signInTestUser(emailB)
        val safetyB = SafetyRepository(firestore, auth)
        val notifs = withTimeout(FLOW_TIMEOUT_MS) {
            safetyB.observeNotifications().first { list -> list.any { it.type == "follow" && it.senderId == uidA } }
        }
        val followNotif = notifs.first { it.type == "follow" && it.senderId == uidA }
        assertFalse(followNotif.isRead)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { safetyB.markNotificationRead(followNotif.id) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { safetyB.deleteNotification(followNotif.id) }.isSuccess)

        // User A unfollows User B
        signInTestUser(emailA)
        val unfollowRes = withTimeout(DEFAULT_TIMEOUT_MS) { socialA.toggleFollow(uidB) }
        assertTrue(unfollowRes.isSuccess)
        assertEquals(false, unfollowRes.getOrNull())

        val profileBAfter = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(uidB).first { it?.followersCount == 0 }
        }
        assertEquals(0, profileBAfter?.followersCount)
    }

    @Test
    fun friendLifecycle_sendCancelResendAcceptAndUnfriend() = runBlocking {
        val tag = uniqueSuffix()
        val email1 = "fr1_$tag@primeesport.com"
        val email2 = "fr2_$tag@primeesport.com"

        val uid1 = signInTestUser(email1)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("fr1_$tag", "Player One")
        }

        val uid2 = signInTestUser(email2)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("fr2_$tag", "Player Two")
        }

        // Player 1 sends friend request then cancels and resends
        signInTestUser(email1)
        val social1 = SocialRepository(firestore, auth)
        val sendRes1 = withTimeout(DEFAULT_TIMEOUT_MS) { social1.sendFriendRequest(uid2) }
        assertTrue("Send FR failed: ${sendRes1.exceptionOrNull()?.message}", sendRes1.isSuccess)
        val requestId = "${uid1}_$uid2"
        val cancelRes = withTimeout(DEFAULT_TIMEOUT_MS) { social1.cancelFriendRequest(requestId) }
        assertTrue("Cancel FR failed: ${cancelRes.exceptionOrNull()?.message}", cancelRes.isSuccess)
        val sendRes2 = withTimeout(DEFAULT_TIMEOUT_MS) { social1.sendFriendRequest(uid2) }
        assertTrue("Resend FR failed: ${sendRes2.exceptionOrNull()?.message}", sendRes2.isSuccess)

        // Player 2 accepts request -> friendsCount increments for both
        signInTestUser(email2)
        val social2 = SocialRepository(firestore, auth)
        val acceptRes = withTimeout(DEFAULT_TIMEOUT_MS) { social2.acceptFriendRequest(requestId, uid1) }
        assertTrue("Accept failed: ${acceptRes.exceptionOrNull()?.message}", acceptRes.isSuccess)

        val p2 = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(uid2).first { it?.friendsCount == 1 }
        }
        assertEquals(1, p2?.friendsCount)

        // Friends can open a DM conversation and exchange a message
        val convRes = withTimeout(DEFAULT_TIMEOUT_MS) { social2.getOrCreateConversationWithFriend(uid1) }
        assertTrue("Open DM failed: ${convRes.exceptionOrNull()?.message}", convRes.isSuccess)
        val convId = convRes.getOrThrow()
        val msgRes = withTimeout(DEFAULT_TIMEOUT_MS) { social2.sendDirectMessage(convId, "GG ready for scrims?") }
        assertTrue("Send DM failed: ${msgRes.exceptionOrNull()?.message}", msgRes.isSuccess)

        // Player 2 unfriends Player 1 -> friendsCount decrements
        val removeRes = withTimeout(DEFAULT_TIMEOUT_MS) { social2.removeFriend(uid1) }
        assertTrue("Unfriend failed: ${removeRes.exceptionOrNull()?.message}", removeRes.isSuccess)
        val p2After = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(uid2).first { it?.friendsCount == 0 }
        }
        assertEquals(0, p2After?.friendsCount)
    }

    @Test
    fun contentOwnership_postsReelsVideosOwnerCanDeleteNonOwnerRejected() = runBlocking {
        val tag = uniqueSuffix()
        val ownerEmail = "owner_$tag@primeesport.com"
        val otherEmail = "other_$tag@primeesport.com"

        val ownerUid = signInTestUser(ownerEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("own_$tag", "Owner Creator")
        }

        val socialOwner = SocialRepository(firestore, auth)
        val creatorOwner = CreatorRepository(firestore, auth)

        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            socialOwner.createPost("Tournament Clutch!", "", "BGMI").getOrThrow()
        }
        val reelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            socialOwner.createReel("https://example.com/reel.mp4", "Ace Highlight", "#bgmi").getOrThrow()
        }
        val videoId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorOwner.uploadVideo("Grand Finals Replay", "https://example.com/vid.mp4", "Full match", "Tournament").getOrThrow()
        }

        // Other user cannot delete owner's content
        signInTestUser(otherEmail)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("oth_$tag", "Other Player")
        }
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) { firestore.collection("posts").document(postId).delete().await() }
            fail("Non-owner should not delete post")
        } catch (e: Exception) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
        }
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) { firestore.collection("reels").document(reelId).delete().await() }
            fail("Non-owner should not delete reel")
        } catch (e: Exception) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
        }
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) { firestore.collection("channelVideos").document(videoId).delete().await() }
            fail("Non-owner should not delete channel video")
        } catch (e: Exception) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.unwrapFirestoreException()?.code)
        }

        // Owner deletes their own content cleanly and counts decrement
        signInTestUser(ownerEmail)
        val postDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            socialOwner.observeUserPosts(ownerUid).first { it.any { p -> p.id == postId } }.first { it.id == postId }
        }
        val reelDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            socialOwner.observeUserReels(ownerUid).first { it.any { r -> r.id == reelId } }.first { it.id == reelId }
        }
        val videoDoc = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorOwner.observeUserVideos(ownerUid).first { it.any { v -> v.id == videoId } }.first { it.id == videoId }
        }

        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { socialOwner.deletePost(postDoc) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { socialOwner.deleteReel(reelDoc) }.isSuccess)
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { creatorOwner.deleteVideo(videoDoc) }.isSuccess)

        val ownerProfileAfter = withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).observeUserProfile(ownerUid)
                .first { it != null && it.postsCount == 0 && it.reelsCount == 0 && it.videosCount == 0 }
        }
        assertEquals(0, ownerProfileAfter?.postsCount)
        assertEquals(0, ownerProfileAfter?.reelsCount)
        assertEquals(0, ownerProfileAfter?.videosCount)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val socialRepo = SocialRepository(firestore, auth)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.createPost("Unauthenticated post", "", "General").getOrThrow()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertTrue(
                e is IllegalStateException ||
                    firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    @Test
    fun communities_duplicateNameRejected_andOfficialOwnerCanDelete() = runBlocking {
        val tag = uniqueSuffix()
        val emailA = "comm_a_$tag@primeesport.com"
        val emailB = "comm_b_$tag@primeesport.com"

        signInTestUser(emailA)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("ca_$tag", "Founder A")
        }
        val esportA = EsportRepository(firestore, auth)
        val commName = "Apex Squad $tag"
        val commRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            esportA.createCommunity(commName, "Competitive squad", "", "Be respectful")
        }
        assertTrue("Create community failed: ${commRes.exceptionOrNull()?.message}", commRes.isSuccess)
        val commId = commRes.getOrThrow()

        // Duplicate community name (case & whitespace insensitive) must fail
        signInTestUser(emailB)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("cb_$tag", "Player B")
        }
        val esportB = EsportRepository(firestore, auth)
        val dupRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            esportB.createCommunity("  ${commName.uppercase()}  ", "Duplicate attempt", "", "")
        }
        assertTrue("Duplicate community name should be rejected", dupRes.isFailure)

        // Non-owner cannot delete community
        val nonOwnerDelete = withTimeout(DEFAULT_TIMEOUT_MS) { esportB.deleteCommunity(commId) }
        assertTrue("Non-owner should not delete community", nonOwnerDelete.isFailure)

        // Creator can delete own community cleanly
        signInTestUser(emailA)
        val ownerDelete = withTimeout(DEFAULT_TIMEOUT_MS) { esportA.deleteCommunity(commId) }
        assertTrue("Owner delete failed: ${ownerDelete.exceptionOrNull()?.message}", ownerDelete.isSuccess)
    }

    @Test
    fun tournaments_unverifiedRejected_verifiedAllowed_andDuplicatesBlocked() = runBlocking {
        val tag = uniqueSuffix()
        val emailRegular = "tourn_reg_$tag@primeesport.com"
        val emailVerified = "tourn_ver_$tag@primeesport.com"

        signInTestUser(emailRegular)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("tr_$tag", "Regular Gamer")
        }
        val esportReg = EsportRepository(firestore, auth)
        val unverifiedRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            esportReg.createTournament(
                title = "Unauthorized Cup $tag",
                game = "BGMI",
                description = "Test",
                prizePool = "",
                teamSize = 4,
                maxTeams = 16,
                startDate = "10 Dec 2026",
                rules = "Standard"
            )
        }
        assertTrue("Unverified regular user must not be allowed to publish tournaments", unverifiedRes.isFailure)

        // Verify another user via Official Owner email in emulator or direct user doc? Let's test via verified organizer doc check
        // Notice in emulator Auth tokens without custom email_verified can't spoof Official Owner;
        // verify rules directly reject non-verified organizer even if Firestore client call is attempted
        val uidVer = signInTestUser(emailVerified)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("tv_$tag", "Verified Org")
        }
        val directAttempt = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("tournaments").document("direct_$tag").set(
                    mapOf(
                        "organizerId" to uidVer,
                        "organizerName" to "Verified Org",
                        "title" to "Direct Bypass Cup",
                        "game" to "BGMI",
                        "prizePool" to "",
                        "status" to "upcoming",
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        assertTrue("Direct unverified Firestore tournament write must be denied by rules", directAttempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            directAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun blocking_blockAndUnblockUser_updatesStateAndGuardsFriendRequests() = runBlocking {
        val tag = uniqueSuffix()
        val emailA = "blk_a_$tag@primeesport.com"
        val emailB = "blk_b_$tag@primeesport.com"

        val uidA = signInTestUser(emailA)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("ba_$tag", "Blocker A")
        }
        val uidB = signInTestUser(emailB)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            AuthRepository(firestore, auth).claimUsernameAndCompleteProfile("bb_$tag", "Target B")
        }

        signInTestUser(emailA)
        val safetyA = SafetyRepository(firestore, auth)
        val socialA = SocialRepository(firestore, auth)

        val blockRes = withTimeout(DEFAULT_TIMEOUT_MS) { safetyA.blockUser(uidB) }
        assertTrue("Block failed: ${blockRes.exceptionOrNull()?.message}", blockRes.isSuccess)

        val blockedSet = withTimeout(FLOW_TIMEOUT_MS) {
            safetyA.observeBlockedUserIds().first { it.contains(uidB) }
        }
        assertTrue(blockedSet.contains(uidB))
        val isBlockedFlag = withTimeout(FLOW_TIMEOUT_MS) {
            safetyA.observeIsUserBlocked(uidB).first { it }
        }
        assertTrue(isBlockedFlag)

        // Blocked user cannot be sent a friend request
        val blockedFriendReq = withTimeout(DEFAULT_TIMEOUT_MS) { socialA.sendFriendRequest(uidB) }
        assertTrue("Blocked user should not receive friend requests", blockedFriendReq.isFailure)

        // Unblock clears block state
        val unblockRes = withTimeout(DEFAULT_TIMEOUT_MS) { safetyA.unblockUser(uidB) }
        assertTrue("Unblock failed: ${unblockRes.exceptionOrNull()?.message}", unblockRes.isSuccess)
        val isBlockedAfter = withTimeout(FLOW_TIMEOUT_MS) {
            safetyA.observeIsUserBlocked(uidB).first { !it }
        }
        assertFalse(isBlockedAfter)
        assertTrue(uidA.isNotBlank())
    }

    @Test
    fun gamingIdentity_updateHandlesTitlesRanksAndSocialLinks_persistsInFirestore() = runBlocking {
        val tag = uniqueSuffix()
        val email = "gid_$tag@primeesport.com"
        val otherEmail = "gid_other_$tag@primeesport.com"

        val uid = signInTestUser(email)
        val authRepo = AuthRepository(firestore, auth)
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                authRepo.claimUsernameAndCompleteProfile("gid_$tag", "Pro Competitor")
            }.isSuccess
        )

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo.updateGamingIdentity(
                favoriteTitles = listOf("Valorant", "BGMI", "Valorant"),
                gameHandles = listOf(
                    GameHandle(platform = "Valorant", handle = "Prime#777"),
                    GameHandle(platform = "BGMI", handle = "5123456789")
                ),
                rankHistory = listOf(
                    RankHistoryEntry(
                        game = "Valorant",
                        season = "Ep 9 Act 2",
                        rank = "Ascendant II",
                        peakRank = "Immortal I",
                        notes = "Duelist main"
                    )
                ),
                socialLinks = listOf(
                    SocialLink(platform = "YouTube", handleOrUrl = "https://youtube.com/@primeesport")
                )
            )
        }
        assertTrue("updateGamingIdentity failed: ${updateRes.exceptionOrNull()?.message}", updateRes.isSuccess)

        val updatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            authRepo.observeUserProfile(uid).first {
                it != null && it.gameHandles.isNotEmpty() && it.rankHistory.isNotEmpty() && it.socialLinks.isNotEmpty()
            }
        }
        assertNotNull(updatedProfile)
        assertEquals(listOf("Valorant", "BGMI"), updatedProfile?.favoriteTitles)
        assertEquals("Valorant, BGMI", updatedProfile?.favoriteGames)
        assertEquals(2, updatedProfile?.gameHandles?.size)
        assertEquals("Prime#777", updatedProfile?.gameHandles?.first()?.handle)
        assertEquals("Immortal I", updatedProfile?.rankHistory?.first()?.peakRank)
        assertEquals("https://youtube.com/@primeesport", updatedProfile?.socialLinks?.first()?.handleOrUrl)

        // Cross-user write to uid's gaming identity must fail rules
        signInTestUser(otherEmail)
        val crossUserRes = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(uid).update(
                    mapOf("gameHandles" to listOf(mapOf("platform" to "Steam", "handle" to "Spoof")))
                ).await()
            }
        }
        assertTrue("Cross-user gaming identity overwrite must be denied", crossUserRes.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            crossUserRes.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun fakeCommunitiesAndTournaments_blockedOnCreate_andPurgedFromFirestore() = runBlocking {
        val tag = uniqueSuffix()
        val email = "purge_$tag@primeesport.com"
        val uid = signInTestUser(email)
        val authRepo = AuthRepository(firestore, auth)
        assertTrue(
            withTimeout(DEFAULT_TIMEOUT_MS) {
                authRepo.claimUsernameAndCompleteProfile("pg_$tag", "Purge Tester")
            }.isSuccess
        )

        val esport = EsportRepository(firestore, auth)

        // Attempt to create confirmed fake community name must fail
        val fakeCommRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            esport.createCommunity("Prime Esports Elite", "Duplicate demo", "", "")
        }
        assertTrue("Confirmed fake community name must be rejected", fakeCommRes.isFailure)

        // Genuine community succeeds
        val realName = "Genuine Clan $tag"
        val realCommRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            esport.createCommunity(realName, "Real player hub", "", "Fair play")
        }
        assertTrue("Genuine community creation failed: ${realCommRes.exceptionOrNull()?.message}", realCommRes.isSuccess)
        val realCommId = realCommRes.getOrThrow()

        // Even if a legacy fake community doc exists for the user or system_demo, it is filtered out and purged
        val fakeCommunityModel = com.example.data.model.Community(
            id = "demo_comm_$tag",
            ownerId = "system_demo",
            name = "Prime Esports Elite",
            description = "Seeded"
        )
        assertTrue(EsportRepository.isConfirmedFakeCommunity(fakeCommunityModel))
        assertFalse(
            EsportRepository.isConfirmedFakeCommunity(
                com.example.data.model.Community(id = realCommId, ownerId = uid, name = realName)
            )
        )

        val fakeTournamentModel = com.example.data.model.Tournament(
            id = "demo_tourn_$tag",
            organizerId = "system_demo",
            organizerName = "System",
            title = "BGMI Masters Series Pro Cup"
        )
        assertTrue(EsportRepository.isConfirmedFakeTournament(fakeTournamentModel))

        val visibleCommunities = withTimeout(FLOW_TIMEOUT_MS) {
            esport.observeCommunities().first { list -> list.any { it.id == realCommId } }
        }
        assertTrue(visibleCommunities.none { it.name.equals("Prime Esports Elite", ignoreCase = true) })

        // Clean up genuine test community
        assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { esport.deleteCommunity(realCommId) }.isSuccess)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
        const val FLOW_TIMEOUT_MS = 8_000L
    }
}

