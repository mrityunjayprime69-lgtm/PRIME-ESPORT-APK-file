package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.Community
import com.example.data.model.CommunityChannel
import com.example.data.model.CommunityMember
import com.example.data.model.CommunityMessage
import com.example.data.model.Tournament
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.model.asUiDataState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

class EsportRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    companion object {
        private val FAKE_SYSTEM_OWNER_IDS = setOf(
            "system_demo", "system", "admin", "demo", "sample", "fake",
            "seed", "mock", "placeholder", "test_user", "bot", "prime_official"
        )
        private val FAKE_ID_PREFIX_REGEX = Regex("^(demo|fake|sample|seed|mock|test|placeholder|system)(_|-).*")
        private val FAKE_ORGANIZER_NAMES = setOf(
            "system", "demo", "sample", "fake", "test organizer", "placeholder", "bot"
        )
        val CONFIRMED_FAKE_COMMUNITY_NAMES = setOf(
            "prime esports elite",
            "prime esport elite",
            "demo community",
            "sample community",
            "fake community",
            "test community"
        )
        val CONFIRMED_FAKE_TOURNAMENT_TITLES = setOf(
            "bgmi masters series pro cup",
            "demo tournament",
            "sample tournament",
            "fake tournament",
            "test tournament"
        )

        fun isFakeOrSystemId(value: String): Boolean {
            val normalized = value.trim().lowercase()
            return normalized in FAKE_SYSTEM_OWNER_IDS || FAKE_ID_PREFIX_REGEX.matches(normalized)
        }

        fun isConfirmedFakeCommunity(
            community: Community,
            usersMap: Map<String, UserProfile> = emptyMap()
        ): Boolean {
            val cleanName = community.name.trim().lowercase()
            val cleanOwner = community.ownerId.trim()
            val cleanDesc = community.description.trim().lowercase()
            if (cleanName.isBlank() || cleanOwner.isBlank()) return true
            if (isFakeOrSystemId(community.id) || isFakeOrSystemId(cleanOwner)) return true
            if (cleanName in CONFIRMED_FAKE_COMMUNITY_NAMES) return true
            if (cleanDesc.contains("lorem ipsum")) return true
            if (usersMap.isNotEmpty() && !usersMap.containsKey(cleanOwner)) return true
            return false
        }

        fun isConfirmedFakeTournament(
            tournament: Tournament,
            usersMap: Map<String, UserProfile> = emptyMap()
        ): Boolean {
            val cleanTitle = tournament.title.trim().lowercase()
            val cleanOrgId = tournament.organizerId.trim()
            val cleanOrgName = tournament.organizerName.trim().lowercase()
            val cleanDesc = tournament.description.trim().lowercase()
            if (cleanTitle.isBlank() || cleanOrgId.isBlank()) return true
            if (isFakeOrSystemId(tournament.id) || isFakeOrSystemId(cleanOrgId)) return true
            if (cleanTitle in CONFIRMED_FAKE_TOURNAMENT_TITLES) return true
            if (cleanOrgName in FAKE_ORGANIZER_NAMES) return true
            if (cleanDesc.contains("lorem ipsum")) return true
            if (usersMap.isNotEmpty()) {
                val organizer = usersMap[cleanOrgId] ?: return true
                if (!organizer.isOwner && !organizer.isVerified) return true
            }
            return false
        }
    }

    private val safetyRepository by lazy { SafetyRepository(db, auth) }
    private val communityMutex = Mutex()
    private val creatingCommunityKeys = mutableSetOf<String>()
    private val tournamentMutex = Mutex()
    private val creatingTournamentKeys = mutableSetOf<String>()
    private val purgeMutex = Mutex()
    private var lastPurgedUserId: String? = null

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isCurrentUserOfficialOwner(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(AuthRepository.OFFICIAL_OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    private fun observeUsersVerificationMap(): Flow<Map<String, UserProfile>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.let { profile ->
                        doc.id to profile
                    }
                }.toMap()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyMap())
            }
    }

    suspend fun purgeFakeAndDuplicateEsportRecords(force: Boolean = false): Int {
        val uid = auth.currentUser?.uid ?: return 0
        val shouldRun = purgeMutex.withLock {
            if (!force && lastPurgedUserId == uid) {
                false
            } else {
                lastPurgedUserId = uid
                true
            }
        }
        if (!shouldRun) return 0

        var deletedCount = 0
        try {
            val usersMap = try {
                db.collection("users").get().await().documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.let { doc.id to it }
                }.toMap()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                emptyMap()
            }

            val isOfficialOwner = isCurrentUserOfficialOwner()

            val communitiesSnap = db.collection("communities").get().await()
            val communities = communitiesSnap.toObjects(Community::class.java)
                .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            val seenCommunityNames = mutableSetOf<String>()
            for (community in communities) {
                if (community.id.isBlank()) continue
                val normName = community.name.trim().lowercase()
                val isFake = isConfirmedFakeCommunity(community, usersMap)
                val isDuplicate = !isFake && normName.isNotBlank() && !seenCommunityNames.add(normName)
                if (isFake || isDuplicate) {
                    val canDelete = isOfficialOwner ||
                        community.ownerId == uid ||
                        isFakeOrSystemId(community.ownerId) ||
                        normName in CONFIRMED_FAKE_COMMUNITY_NAMES
                    if (canDelete) {
                        try {
                            db.collection("communities").document(community.id).delete().await()
                            deletedCount++
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            val tournamentsSnap = db.collection("tournaments").get().await()
            val tournaments = tournamentsSnap.toObjects(Tournament::class.java)
                .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            val seenTournamentKeys = mutableSetOf<String>()
            for (tournament in tournaments) {
                if (tournament.id.isBlank()) continue
                val normKey = "${tournament.title.trim().lowercase()}|${tournament.startDate.trim().lowercase()}"
                val isFake = isConfirmedFakeTournament(tournament, usersMap)
                val isDuplicate = !isFake && tournament.title.isNotBlank() && !seenTournamentKeys.add(normKey)
                if (isFake || isDuplicate) {
                    val canDelete = isOfficialOwner ||
                        tournament.organizerId == uid ||
                        isFakeOrSystemId(tournament.organizerId) ||
                        tournament.title.trim().lowercase() in CONFIRMED_FAKE_TOURNAMENT_TITLES
                    if (canDelete) {
                        try {
                            db.collection("tournaments").document(tournament.id).delete().await()
                            deletedCount++
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
        }
        return deletedCount
    }

    // --- Tournaments ---
    private fun observeTournamentsRaw(): Flow<List<Tournament>> {
        val tournamentsFlow = db.collection("tournaments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Tournament::class.java) }
        return combine(tournamentsFlow, observeUsersVerificationMap()) { tournaments, usersMap ->
            tournaments
                .filter { !isConfirmedFakeTournament(it, usersMap) }
                .distinctBy { "${it.title.trim().lowercase()}|${it.startDate.trim().lowercase()}" }
        }
    }

    fun observeTournamentsState(): Flow<UiDataState<List<Tournament>>> {
        return observeTournamentsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "tournaments")
            friendlyFirestoreError(error, "tournaments")
        }
    }

    fun observeTournaments(): Flow<List<Tournament>> {
        return observeTournamentsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "tournaments")
                emit(emptyList())
            }
    }

    suspend fun createTournament(
        title: String,
        game: String,
        description: String,
        prizePool: String,
        teamSize: Int,
        maxTeams: Int,
        startDate: String,
        rules: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanTitle = title.trim()
        val cleanGame = game.trim()
        val cleanStartDate = startDate.trim()
        if (cleanTitle.length < 3) {
            return Result.failure(IllegalArgumentException("Tournament title must be at least 3 characters."))
        }
        if (cleanTitle.lowercase() in CONFIRMED_FAKE_TOURNAMENT_TITLES) {
            return Result.failure(IllegalArgumentException("Please choose a unique, genuine tournament title."))
        }
        if (cleanGame.length < 2) {
            return Result.failure(IllegalArgumentException("Please select a valid game."))
        }

        val dedupKey = "${cleanTitle.lowercase()}|${cleanStartDate.lowercase()}"
        val acquired = tournamentMutex.withLock {
            creatingTournamentKeys.add(dedupKey)
        }
        if (!acquired) {
            return Result.failure(IllegalStateException("Tournament submission is already in progress."))
        }

        val tournamentId = UUID.randomUUID().toString()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val isAuthorizedOrganizer = isCurrentUserOfficialOwner() ||
                userDoc.getBoolean("isOwner") == true ||
                userDoc.getBoolean("isVerified") == true
            if (!isAuthorizedOrganizer) {
                return Result.failure(
                    SecurityException("Only verified creators and official organizers can publish tournaments.")
                )
            }

            val organizerName = userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: userDoc.getString("username")?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Complete your organizer profile before hosting a tournament."))

            val existingSnapshot = db.collection("tournaments").get().await()
            val hasDuplicate = existingSnapshot.documents.any { doc ->
                val existingTitle = doc.getString("title")?.trim() ?: ""
                val existingDate = doc.getString("startDate")?.trim() ?: ""
                existingTitle.equals(cleanTitle, ignoreCase = true) &&
                    existingDate.equals(cleanStartDate, ignoreCase = true)
            }
            if (hasDuplicate) {
                return Result.failure(IllegalStateException("A tournament with this title and schedule already exists."))
            }

            val payload = mapOf(
                "organizerId" to uid,
                "organizerName" to organizerName.take(50),
                "title" to cleanTitle.take(80),
                "game" to cleanGame.take(50),
                "description" to description.trim().take(1000),
                "prizePool" to prizePool.trim().take(50),
                "teamSize" to teamSize,
                "maxTeams" to maxTeams,
                "registeredTeamsCount" to 0,
                "startDate" to cleanStartDate.take(50),
                "status" to "upcoming",
                "rules" to rules.trim().take(2000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("tournaments").document(tournamentId).set(payload).await()
            Result.success(tournamentId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tournaments/$tournamentId")
            Result.failure(e)
        } finally {
            tournamentMutex.withLock {
                creatingTournamentKeys.remove(dedupKey)
            }
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val snap = db.collection("tournaments").document(tournamentId).get().await()
            if (snap.exists() && snap.getString("organizerId") != uid && !isCurrentUserOfficialOwner()) {
                return Result.failure(SecurityException("Only the tournament organizer or Official Owner can delete it."))
            }
            db.collection("tournaments").document(tournamentId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tournaments/$tournamentId")
            Result.failure(e)
        }
    }

    suspend fun registerForTournament(tournamentId: String, teamName: String, playerUsernames: String): Result<Unit> {
        val uid = requireUserId()
        val registrationId = "${tournamentId}_$uid"
        return try {
            val tournamentSnap = db.collection("tournaments").document(tournamentId).get().await()
            val organizerId = tournamentSnap.getString("organizerId") ?: ""
            val title = tournamentSnap.getString("title") ?: "Tournament"

            val existing = db.collection("tournaments").document(tournamentId)
                .collection("registrations").document(registrationId).get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("You have already registered a team for this tournament."))
            }

            val payload = mapOf(
                "tournamentId" to tournamentId,
                "userId" to uid,
                "teamName" to teamName.trim(),
                "playerUsernames" to playerUsernames.trim(),
                "status" to "confirmed",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("tournaments").document(tournamentId).collection("registrations").document(registrationId).set(payload).await()
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "registeredTeamsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (organizerId.isNotBlank() && organizerId != uid) {
                safetyRepository.createNotification(
                    recipientUserId = organizerId,
                    title = "New Tournament Team",
                    body = "Team '${teamName.trim()}' registered for $title.",
                    type = "tournament",
                    targetId = tournamentId,
                    dedupId = "treg_${tournamentId}_$uid"
                )
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tournaments/$tournamentId/registrations/$registrationId")
            Result.failure(e)
        }
    }

    // --- Communities ---
    private fun observeCommunitiesRaw(): Flow<List<Community>> {
        val communitiesFlow = db.collection("communities")
            .orderBy("memberCount", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Community::class.java) }
        return combine(communitiesFlow, observeUsersVerificationMap()) { communities, usersMap ->
            communities
                .filter { !isConfirmedFakeCommunity(it, usersMap) }
                .distinctBy { it.name.trim().lowercase() }
        }
    }

    fun observeCommunitiesState(): Flow<UiDataState<List<Community>>> {
        return observeCommunitiesRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "communities")
            friendlyFirestoreError(error, "communities")
        }
    }

    fun observeCommunities(): Flow<List<Community>> {
        return observeCommunitiesRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "communities")
                emit(emptyList())
            }
    }

    suspend fun createCommunity(name: String, description: String, iconUrl: String, rules: String): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Community name must be at least 2 characters."))
        }
        if (cleanName.lowercase() in CONFIRMED_FAKE_COMMUNITY_NAMES) {
            return Result.failure(IllegalArgumentException("Please choose a unique, genuine community name."))
        }

        val normalizedName = cleanName.lowercase()
        val acquired = communityMutex.withLock {
            creatingCommunityKeys.add(normalizedName)
        }
        if (!acquired) {
            return Result.failure(IllegalStateException("Community creation is already in progress."))
        }

        val communityId = UUID.randomUUID().toString()
        return try {
            val existingCommunities = db.collection("communities").get().await()
            val duplicateExists = existingCommunities.documents.any { doc ->
                val existingName = doc.getString("name")?.trim()?.lowercase() ?: ""
                existingName == normalizedName
            }
            if (duplicateExists) {
                return Result.failure(IllegalStateException("A community named '$cleanName' already exists."))
            }

            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username")?.takeIf { it.isNotBlank() }
                ?: userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Complete your profile before creating a community."))
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            val batch = db.batch()
            val communityRef = db.collection("communities").document(communityId)
            batch.set(
                communityRef,
                mapOf(
                    "ownerId" to uid,
                    "name" to cleanName.take(60),
                    "description" to description.trim().take(1000),
                    "iconUrl" to iconUrl.trim().take(1000),
                    "memberCount" to 1,
                    "isPublic" to true,
                    "rules" to rules.trim().take(1500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                communityRef.collection("members").document(uid),
                mapOf(
                    "communityId" to communityId,
                    "userId" to uid,
                    "username" to username,
                    "avatarUrl" to avatarUrl,
                    "role" to "owner",
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                communityRef.collection("channels").document("general"),
                mapOf(
                    "communityId" to communityId,
                    "name" to "general-chat",
                    "topic" to "General discussions",
                    "type" to "gaming",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(communityId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communities/$communityId")
            Result.failure(e)
        } finally {
            communityMutex.withLock {
                creatingCommunityKeys.remove(normalizedName)
            }
        }
    }

    suspend fun deleteCommunity(communityId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val snap = db.collection("communities").document(communityId).get().await()
            if (snap.exists() && snap.getString("ownerId") != uid && !isCurrentUserOfficialOwner()) {
                return Result.failure(SecurityException("Only the community owner or Official Owner can delete it."))
            }
            db.collection("communities").document(communityId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "communities/$communityId")
            Result.failure(e)
        }
    }

    suspend fun inviteUserToCommunity(communityId: String, communityName: String, targetUserId: String): Result<Unit> {
        return safetyRepository.createNotification(
            recipientUserId = targetUserId,
            title = "Community Invite",
            body = "You were invited to join '$communityName' on PRIME ESPORT.",
            type = "community_invite",
            targetId = communityId,
            dedupId = "cinvite_${communityId}_$targetUserId"
        )
    }

    suspend fun joinCommunity(communityId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: userDoc.getString("displayName") ?: "Player"
            val memberRef = db.collection("communities").document(communityId).collection("members").document(uid)
            val existing = memberRef.get().await()
            if (existing.exists()) {
                return Result.success(Unit)
            }

            memberRef.set(
                mapOf(
                    "communityId" to communityId,
                    "userId" to uid,
                    "username" to username,
                    "avatarUrl" to (userDoc.getString("avatarUrl") ?: ""),
                    "role" to "member",
                    "joinedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("communities").document(communityId).update(
                mapOf(
                    "memberCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communities/$communityId/members/$uid")
            Result.failure(e)
        }
    }

    fun observeCommunityMembers(communityId: String): Flow<List<CommunityMember>> {
        val path = "communities/$communityId/members"
        return db.collection(path)
            .snapshots()
            .map { it.toObjects(CommunityMember::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeCommunityChannels(communityId: String): Flow<List<CommunityChannel>> {
        val path = "communities/$communityId/channels"
        return db.collection(path)
            .snapshots()
            .map { it.toObjects(CommunityChannel::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private fun observeCommunityMessagesRaw(communityId: String, channelId: String): Flow<List<CommunityMessage>> {
        return db.collection("communities/$communityId/channels/$channelId/messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(CommunityMessage::class.java) }
    }

    fun observeCommunityMessagesState(communityId: String, channelId: String): Flow<UiDataState<List<CommunityMessage>>> {
        val path = "communities/$communityId/channels/$channelId/messages"
        return observeCommunityMessagesRaw(communityId, channelId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, path)
            friendlyFirestoreError(error, "community messages")
        }
    }

    fun observeCommunityMessages(communityId: String, channelId: String): Flow<List<CommunityMessage>> {
        val path = "communities/$communityId/channels/$channelId/messages"
        return observeCommunityMessagesRaw(communityId, channelId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendCommunityMessage(communityId: String, channelId: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val messageId = UUID.randomUUID().toString()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: userDoc.getString("username")
                ?: "Player"

            val payload = mapOf(
                "communityId" to communityId,
                "channelId" to channelId,
                "authorId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to (userDoc.getString("avatarUrl") ?: ""),
                "text" to text.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("communities").document(communityId).collection("channels").document(channelId).collection("messages").document(messageId).set(payload).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communities/$communityId/channels/$channelId/messages/$messageId")
            Result.failure(e)
        }
    }
}
