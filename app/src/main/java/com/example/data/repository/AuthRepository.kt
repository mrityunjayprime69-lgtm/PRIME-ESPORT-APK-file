package com.example.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.GameHandle
import com.example.data.model.RankHistoryEntry
import com.example.data.model.SocialLink
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.model.asUiDataState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface UsernameCheckResult {
    object Empty : UsernameCheckResult
    data class InvalidFormat(val message: String) : UsernameCheckResult
    data class Reserved(val message: String) : UsernameCheckResult
    object Checking : UsernameCheckResult
    data class Available(val normalized: String) : UsernameCheckResult
    data class Taken(val message: String) : UsernameCheckResult
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    companion object {
        const val OFFICIAL_OWNER_EMAIL = "sbb521523@gmail.com"
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val reservedUsernames = setOf(
        "admin", "administrator", "support", "prime", "primeesport", "primegaming",
        "mod", "moderator", "official", "system", "help", "staff", "root", "null", "undefined"
    )

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCurrentUserOfficialOwner(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(OFFICIAL_OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    suspend fun refreshProfileOnStartup(userId: String): Result<Boolean> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val hasUsername = doc.exists() && !doc.getString("username").isNullOrBlank()
            if (hasUsername && isCurrentUserOfficialOwner()) {
                val isOwner = doc.getBoolean("isOwner") ?: false
                val isVerified = doc.getBoolean("isVerified") ?: false
                if (!isOwner || !isVerified) {
                    try {
                        db.collection("users").document(userId).update(
                            mapOf(
                                "isOwner" to true,
                                "isVerified" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (_: Exception) {}
                }
            }
            Result.success(hasUsername)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun hasUsername(userId: String): Boolean {
        return refreshProfileOnStartup(userId).getOrDefault(false)
    }

    suspend fun checkUsernameAvailability(rawUsername: String): UsernameCheckResult {
        val trimmed = rawUsername.trim()
        if (trimmed.isEmpty()) {
            return UsernameCheckResult.Empty
        }

        if (trimmed.length < 3) {
            return UsernameCheckResult.InvalidFormat("Username must be at least 3 characters long.")
        }

        if (trimmed.length > 20) {
            return UsernameCheckResult.InvalidFormat("Username cannot exceed 20 characters.")
        }

        val regex = Regex("^[a-zA-Z0-9_]+$")
        if (!regex.matches(trimmed)) {
            return UsernameCheckResult.InvalidFormat("Only letters, numbers, and underscores are allowed.")
        }

        val normalized = trimmed.lowercase()
        if (reservedUsernames.contains(normalized)) {
            return UsernameCheckResult.Reserved("'$trimmed' is a reserved system handle.")
        }

        return try {
            val uid = auth.currentUser?.uid
            val doc = db.collection("usernames").document(normalized).get().await()
            if (doc.exists()) {
                val ownerUid = doc.getString("uid")
                if (ownerUid != null && ownerUid == uid) {
                    UsernameCheckResult.Available(normalized)
                } else {
                    UsernameCheckResult.Taken("Username '@$trimmed' is already taken.")
                }
            } else {
                UsernameCheckResult.Available(normalized)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            UsernameCheckResult.InvalidFormat(e.localizedMessage ?: "Failed to verify username availability.")
        }
    }

    suspend fun claimUsernameAndCompleteProfile(
        rawUsername: String,
        rawDisplayName: String,
        avatarUrl: String = "",
        bio: String = "",
        favoriteGames: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val trimmed = rawUsername.trim()
        val normalized = trimmed.lowercase()

        val availability = checkUsernameAvailability(trimmed)
        if (availability !is UsernameCheckResult.Available) {
            val msg = when (availability) {
                is UsernameCheckResult.Taken -> availability.message
                is UsernameCheckResult.Reserved -> availability.message
                is UsernameCheckResult.InvalidFormat -> availability.message
                else -> "Invalid username specified."
            }
            return Result.failure(IllegalArgumentException(msg))
        }

        val finalDisplayName = rawDisplayName.trim().ifEmpty { trimmed }
        val finalBio = bio.trim()
        val finalGames = favoriteGames.trim()
        val finalAvatar = avatarUrl.trim().ifEmpty { auth.currentUser?.photoUrl?.toString() ?: "" }
        val ownerPrivilege = isCurrentUserOfficialOwner()

        return try {
            db.runTransaction { transaction ->
                val usernameDocRef = db.collection("usernames").document(normalized)
                val existingUsernameSnapshot = transaction.get(usernameDocRef)

                if (existingUsernameSnapshot.exists()) {
                    val existingOwnerUid = existingUsernameSnapshot.getString("uid")
                    if (existingOwnerUid != null && existingOwnerUid != uid) {
                        throw IllegalStateException("Username '@$trimmed' was just claimed by another user.")
                    }
                }

                transaction.set(
                    usernameDocRef,
                    mapOf(
                        "uid" to uid,
                        "username" to trimmed,
                        "normalized" to normalized,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                val parsedFavorites = finalGames.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(20)
                val userDocRef = db.collection("users").document(uid)
                val userPayload = mapOf(
                    "userId" to uid,
                    "username" to trimmed,
                    "displayName" to finalDisplayName,
                    "avatarUrl" to finalAvatar,
                    "bannerUrl" to "",
                    "bio" to finalBio,
                    "favoriteGames" to finalGames,
                    "favoriteTitles" to parsedFavorites,
                    "gameHandles" to emptyList<Map<String, String>>(),
                    "rankHistory" to emptyList<Map<String, String>>(),
                    "socialLinks" to emptyList<Map<String, String>>(),
                    "postsCount" to 0,
                    "videosCount" to 0,
                    "reelsCount" to 0,
                    "followersCount" to 0,
                    "followingCount" to 0,
                    "friendsCount" to 0,
                    "isVerified" to ownerPrivilege,
                    "isOwner" to ownerPrivilege,
                    "isOnline" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                transaction.set(userDocRef, userPayload)
            }.await()

            val email = auth.currentUser?.email ?: ""
            db.collection("users").document(uid).collection("private").document("info").set(
                mapOf(
                    "email" to email,
                    "isPrivate" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "usernames/$normalized")
            Result.failure(e)
        }
    }

    private fun observeUserProfileRaw(userId: String): Flow<UserProfile?> {
        return db.collection("users")
            .document(userId)
            .snapshots()
            .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
    }

    fun observeUserProfileState(userId: String): Flow<UiDataState<UserProfile?>> {
        val path = "users/$userId"
        return observeUserProfileRaw(userId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.GET, path)
            friendlyFirestoreError(error, "player profile")
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return observeUserProfileRaw(userId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    private fun observeUsersMapRaw(): Flow<Map<String, UserProfile>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java)
                    .filter { it.userId.isNotBlank() }
                    .associateBy { it.userId }
            }
    }

    fun observeUsersMapState(): Flow<UiDataState<Map<String, UserProfile>>> {
        return observeUsersMapRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "users")
            friendlyFirestoreError(error, "players")
        }
    }

    fun observeUsersMap(): Flow<Map<String, UserProfile>> {
        val path = "users"
        return observeUsersMapRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyMap())
            }
    }

    fun observeAllUsersState(): Flow<UiDataState<List<UserProfile>>> {
        return db.collection("users")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(UserProfile::class.java).filter { it.userId.isNotBlank() } }
            .asUiDataState { error ->
                handleFirestoreError(error, OperationType.LIST, "users")
                friendlyFirestoreError(error, "players")
            }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(UserProfile::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun updateUserProfile(
        displayName: String,
        bio: String,
        avatarUrl: String,
        favoriteGames: String,
        bannerUrl: String? = null,
        newUsername: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val cleanDisplay = displayName.trim()
        if (cleanDisplay.isEmpty()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty."))
        }

        val currentDoc = try {
            db.collection("users").document(uid).get().await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val oldUsername = currentDoc.getString("username") ?: ""
        val oldNormalized = oldUsername.trim().lowercase()
        val requestedUsername = newUsername?.trim()?.takeIf { it.isNotEmpty() } ?: oldUsername
        val newNormalized = requestedUsername.lowercase()
        val isChangingUsername = newNormalized != oldNormalized

        if (isChangingUsername) {
            val check = checkUsernameAvailability(requestedUsername)
            if (check !is UsernameCheckResult.Available) {
                val msg = when (check) {
                    is UsernameCheckResult.Taken -> check.message
                    is UsernameCheckResult.Reserved -> check.message
                    is UsernameCheckResult.InvalidFormat -> check.message
                    else -> "Invalid username."
                }
                return Result.failure(IllegalArgumentException(msg))
            }
        }

        val cleanAvatar = avatarUrl.trim()
        val cleanBanner = bannerUrl?.trim() ?: (currentDoc.getString("bannerUrl") ?: "")

        return try {
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                if (isChangingUsername) {
                    val newUsernameRef = db.collection("usernames").document(newNormalized)
                    val newSnap = transaction.get(newUsernameRef)
                    if (newSnap.exists()) {
                        val ownerUid = newSnap.getString("uid")
                        if (ownerUid != null && ownerUid != uid) {
                            throw IllegalStateException("Username '@$requestedUsername' is already taken.")
                        }
                    }
                    if (oldNormalized.isNotBlank()) {
                        val oldUsernameRef = db.collection("usernames").document(oldNormalized)
                        transaction.delete(oldUsernameRef)
                    }
                    transaction.set(
                        newUsernameRef,
                        mapOf(
                            "uid" to uid,
                            "username" to requestedUsername,
                            "normalized" to newNormalized,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val cleanGames = favoriteGames.trim().take(200)
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanDisplay,
                    "bio" to bio.trim().take(300),
                    "avatarUrl" to cleanAvatar,
                    "bannerUrl" to cleanBanner,
                    "favoriteGames" to cleanGames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanGames.isNotBlank()) {
                    val titles = cleanGames.split(",")
                        .map { it.trim().take(40) }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .take(20)
                    updates["favoriteTitles"] = titles
                }
                if (isChangingUsername || requestedUsername != oldUsername) {
                    updates["username"] = requestedUsername
                }
                transaction.update(userRef, updates)
            }.await()

            propagateAuthorMetadata(uid, requestedUsername, cleanAvatar)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGamingIdentity(
        favoriteTitles: List<String>,
        gameHandles: List<GameHandle>,
        rankHistory: List<RankHistoryEntry>,
        socialLinks: List<SocialLink>
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"

        val cleanTitles = favoriteTitles
            .map { it.trim().take(40) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(20)

        val cleanHandles = gameHandles
            .mapNotNull { entry ->
                val platform = entry.platform.trim().take(30)
                val handle = entry.handle.trim().take(50)
                if (platform.isEmpty() || handle.isEmpty()) null
                else mapOf("platform" to platform, "handle" to handle)
            }
            .distinctBy { "${it["platform"]?.lowercase()}|${it["handle"]?.lowercase()}" }
            .take(15)

        val cleanRanks = rankHistory
            .mapNotNull { entry ->
                val game = entry.game.trim().take(40)
                val rank = entry.rank.trim().take(40)
                if (game.isEmpty() || rank.isEmpty()) null
                else mapOf(
                    "game" to game,
                    "season" to entry.season.trim().take(40),
                    "rank" to rank,
                    "peakRank" to entry.peakRank.trim().take(40),
                    "notes" to entry.notes.trim().take(120)
                )
            }
            .take(25)

        val cleanSocials = socialLinks
            .mapNotNull { entry ->
                val platform = entry.platform.trim().take(30)
                val value = entry.handleOrUrl.trim().take(200)
                if (platform.isEmpty() || value.isEmpty()) null
                else mapOf("platform" to platform, "handleOrUrl" to value)
            }
            .distinctBy { "${it["platform"]?.lowercase()}|${it["handleOrUrl"]?.lowercase()}" }
            .take(12)

        val syncedGamesSummary = cleanTitles.joinToString(", ").take(200)

        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "favoriteTitles" to cleanTitles,
                    "favoriteGames" to syncedGamesSummary,
                    "gameHandles" to cleanHandles,
                    "rankHistory" to cleanRanks,
                    "socialLinks" to cleanSocials,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun propagateAuthorMetadata(uid: String, username: String, avatarUrl: String) {
        try {
            val postDocs = db.collection("posts").whereEqualTo("authorId", uid).get().await()
            val reelDocs = db.collection("reels").whereEqualTo("authorId", uid).get().await()
            val videoDocs = db.collection("channelVideos").whereEqualTo("ownerId", uid).get().await()

            val allRefs = (postDocs.documents + reelDocs.documents + videoDocs.documents).map { it.reference }
            allRefs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref ->
                    batch.update(
                        ref,
                        mapOf(
                            "authorUsername" to username,
                            "authorAvatarUrl" to avatarUrl
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {}
    }

    suspend fun setUserVerified(targetUserId: String, verified: Boolean): Result<Unit> {
        if (!isCurrentUserOfficialOwner()) {
            return Result.failure(SecurityException("Only the Official Owner can manage creator verification."))
        }
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "isVerified" to verified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (verified && targetUserId != auth.currentUser?.uid) {
                val safety = SafetyRepository(db, auth)
                safety.createNotification(
                    recipientUserId = targetUserId,
                    title = "Account Verified",
                    body = "Congratulations! Your PRIME ESPORT creator profile is now verified.",
                    type = "moderation",
                    targetId = targetUserId
                )
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun signOut(@Suppress("UNUSED_PARAMETER") context: Context, credentialManager: CredentialManager) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            auth.signOut()
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("No authenticated user"))
        return try {
            val uid = user.uid
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username")
            if (!username.isNullOrBlank()) {
                db.collection("usernames").document(username.lowercase()).delete().await()
            }
            db.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${user.uid}")
            Result.failure(e)
        }
    }
}
