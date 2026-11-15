package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Follow
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.Reel
import com.example.data.model.ReelComment
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.model.asUiDataState
import com.example.data.storage.MediaUploadService
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    private val safetyRepository by lazy { SafetyRepository(db, auth) }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Posts ---
    private fun observePostsRaw(): Flow<List<Post>> {
        return db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(Post::class.java) }
    }

    fun observePostsState(): Flow<UiDataState<List<Post>>> {
        return observePostsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "posts")
            friendlyFirestoreError(error, "posts")
        }
    }

    fun observePosts(): Flow<List<Post>> {
        val path = "posts"
        return observePostsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private fun observeUserPostsRaw(userId: String): Flow<List<Post>> {
        return db.collection("posts")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeUserPostsState(userId: String): Flow<UiDataState<List<Post>>> {
        return observeUserPostsRaw(userId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "posts")
            friendlyFirestoreError(error, "posts")
        }
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> {
        val path = "posts"
        return observeUserPostsRaw(userId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createPost(
        content: String,
        imageUrl: String,
        gameTag: String,
        storagePath: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val cleanContent = content.trim()
        val cleanImage = imageUrl.trim()
        if (cleanContent.isEmpty() && cleanImage.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter text or attach an image."))
        }

        val postId = UUID.randomUUID().toString()
        val path = "posts/$postId"
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "Gamer"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            val payload = mapOf(
                "authorId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to avatarUrl,
                "content" to cleanContent,
                "imageUrl" to cleanImage,
                "storagePath" to storagePath.trim(),
                "gameTag" to gameTag.trim(),
                "likesCount" to 0,
                "commentsCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).set(payload).await()
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "postsCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {}
            Result.success(postId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(
        post: Post,
        mediaUploadService: MediaUploadService? = null
    ): Result<Unit> {
        val uid = requireUserId()
        if (post.authorId != uid) {
            return Result.failure(SecurityException("Only the owner can delete this post."))
        }
        return try {
            db.runTransaction { tx ->
                val postRef = db.collection("posts").document(post.id)
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val currentCount = (userSnap.getLong("postsCount") ?: 0L).toInt()
                tx.delete(postRef)
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "postsCount" to (currentCount - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            if (mediaUploadService != null && (post.storagePath.isNotBlank() || post.imageUrl.isNotBlank())) {
                mediaUploadService.deleteMedia(post.storagePath, post.imageUrl)
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    fun observeIsPostLiked(postId: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(false)
        return db.collection("posts").document(postId)
            .collection("likes").document(uid)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(false)
            }
    }

    suspend fun toggleLikePost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val likeDocRef = postRef.collection("likes").document(uid)
        return try {
            val postSnap = postRef.get().await()
            val authorId = postSnap.getString("authorId") ?: ""
            val currentLikes = (postSnap.getLong("likesCount") ?: 0L).toInt()
            val exists = likeDocRef.get().await().exists()
            if (exists) {
                likeDocRef.delete().await()
                postRef.update("likesCount", (currentLikes - 1).coerceAtLeast(0)).await()
            } else {
                likeDocRef.set(
                    mapOf(
                        "postId" to postId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update("likesCount", currentLikes + 1).await()
                if (authorId.isNotBlank() && authorId != uid) {
                    safetyRepository.createNotification(
                        recipientUserId = authorId,
                        title = "Post Liked",
                        body = "Someone liked your post on PRIME ESPORT.",
                        type = "like",
                        targetId = postId,
                        dedupId = "like_post_${postId}_$uid"
                    )
                }
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts/$postId/likes/$uid")
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> {
        val path = "posts/$postId/comments"
        return db.collection(path)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(PostComment::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun addComment(postId: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val commentId = UUID.randomUUID().toString()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "Gamer"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""
            val postSnap = db.collection("posts").document(postId).get().await()
            val postAuthorId = postSnap.getString("authorId") ?: ""

            val payload = mapOf(
                "postId" to postId,
                "authorId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to avatarUrl,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("posts").document(postId).collection("comments").document(commentId).set(payload).await()
            db.collection("posts").document(postId).update("commentsCount", FieldValue.increment(1)).await()

            if (postAuthorId.isNotBlank() && postAuthorId != uid) {
                safetyRepository.createNotification(
                    recipientUserId = postAuthorId,
                    title = "New Comment",
                    body = "@$username commented: \"${cleanText.take(80)}\"",
                    type = "comment",
                    targetId = postId
                )
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/$postId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val ref = db.collection("posts").document(postId).collection("comments").document(commentId)
            val snap = ref.get().await()
            if (snap.getString("authorId") != uid) {
                return Result.failure(SecurityException("Only the comment author can delete it."))
            }
            ref.delete().await()
            val postSnap = db.collection("posts").document(postId).get().await()
            val count = (postSnap.getLong("commentsCount") ?: 0L).toInt()
            db.collection("posts").document(postId).update("commentsCount", (count - 1).coerceAtLeast(0)).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId/comments/$commentId")
            Result.failure(e)
        }
    }

    // --- Reels ---
    private fun isPlayableRemoteReel(reel: Reel): Boolean {
        val url = reel.videoUrl.trim()
        val isHttp = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        return isHttp && !EsportRepository.isFakeOrSystemId(reel.id) && !EsportRepository.isFakeOrSystemId(reel.authorId)
    }

    private fun observeReelsRaw(): Flow<List<Reel>> {
        return db.collection("reels")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap -> snap.toObjects(Reel::class.java).filter(::isPlayableRemoteReel) }
    }

    fun observeReelsState(): Flow<UiDataState<List<Reel>>> {
        return observeReelsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "reels")
            friendlyFirestoreError(error, "Reels")
        }
    }

    fun observeReels(): Flow<List<Reel>> {
        val path = "reels"
        return observeReelsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private fun observeUserReelsRaw(userId: String): Flow<List<Reel>> {
        return db.collection("reels")
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Reel::class.java)
                    .filter(::isPlayableRemoteReel)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeUserReelsState(userId: String): Flow<UiDataState<List<Reel>>> {
        return observeUserReelsRaw(userId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "reels")
            friendlyFirestoreError(error, "Reels")
        }
    }

    fun observeUserReels(userId: String): Flow<List<Reel>> {
        val path = "reels"
        return observeUserReelsRaw(userId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createReel(
        videoUrl: String,
        caption: String,
        tags: String,
        storagePath: String = "",
        thumbnailUrl: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val reelId = UUID.randomUUID().toString()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "Gamer"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            val payload = mapOf(
                "authorId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to avatarUrl,
                "videoUrl" to videoUrl.trim(),
                "storagePath" to storagePath.trim(),
                "thumbnailUrl" to thumbnailUrl.trim(),
                "caption" to caption.trim(),
                "tags" to tags.trim(),
                "likesCount" to 0,
                "commentsCount" to 0,
                "viewsCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reels").document(reelId).set(payload).await()
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "reelsCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {}
            Result.success(reelId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reels/$reelId")
            Result.failure(e)
        }
    }

    suspend fun deleteReel(
        reel: Reel,
        mediaUploadService: MediaUploadService? = null
    ): Result<Unit> {
        val uid = requireUserId()
        if (reel.authorId != uid) {
            return Result.failure(SecurityException("Only the owner can delete this Reel."))
        }
        return try {
            db.runTransaction { tx ->
                val reelRef = db.collection("reels").document(reel.id)
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val currentCount = (userSnap.getLong("reelsCount") ?: 0L).toInt()
                tx.delete(reelRef)
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "reelsCount" to (currentCount - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            if (mediaUploadService != null && (reel.storagePath.isNotBlank() || reel.videoUrl.isNotBlank())) {
                mediaUploadService.deleteMedia(reel.storagePath, reel.videoUrl)
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "reels/${reel.id}")
            Result.failure(e)
        }
    }

    fun observeIsReelLiked(reelId: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(false)
        return db.collection("reels").document(reelId)
            .collection("likes").document(uid)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(false)
            }
    }

    suspend fun toggleLikeReel(reelId: String): Result<Unit> {
        val uid = requireUserId()
        val reelRef = db.collection("reels").document(reelId)
        val likeRef = reelRef.collection("likes").document(uid)
        return try {
            val reelSnap = reelRef.get().await()
            val authorId = reelSnap.getString("authorId") ?: ""
            val currentLikes = (reelSnap.getLong("likesCount") ?: 0L).toInt()
            val alreadyLiked = likeRef.get().await().exists()

            if (alreadyLiked) {
                likeRef.delete().await()
                reelRef.update("likesCount", (currentLikes - 1).coerceAtLeast(0)).await()
            } else {
                likeRef.set(
                    mapOf(
                        "reelId" to reelId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                reelRef.update("likesCount", currentLikes + 1).await()
                if (authorId.isNotBlank() && authorId != uid) {
                    safetyRepository.createNotification(
                        recipientUserId = authorId,
                        title = "Reel Liked",
                        body = "Someone liked your Reel on PRIME ESPORT.",
                        type = "like",
                        targetId = reelId,
                        dedupId = "like_reel_${reelId}_$uid"
                    )
                }
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reels/$reelId")
            Result.failure(e)
        }
    }

    fun observeReelComments(reelId: String): Flow<List<ReelComment>> {
        val path = "reels/$reelId/comments"
        return db.collection(path)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(ReelComment::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun addReelComment(reelId: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val commentId = UUID.randomUUID().toString()
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "Gamer"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""
            val reelSnap = db.collection("reels").document(reelId).get().await()
            val reelAuthorId = reelSnap.getString("authorId") ?: ""

            val payload = mapOf(
                "reelId" to reelId,
                "authorId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to avatarUrl,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reels").document(reelId).collection("comments").document(commentId).set(payload).await()
            db.collection("reels").document(reelId).update("commentsCount", FieldValue.increment(1)).await()

            if (reelAuthorId.isNotBlank() && reelAuthorId != uid) {
                safetyRepository.createNotification(
                    recipientUserId = reelAuthorId,
                    title = "New Reel Comment",
                    body = "@$username commented: \"${cleanText.take(80)}\"",
                    type = "comment",
                    targetId = reelId
                )
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reels/$reelId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun incrementReelViews(reelId: String) {
        try {
            db.collection("reels").document(reelId).update("viewsCount", FieldValue.increment(1)).await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {}
    }

    // --- Friend Requests & Friendships ---
    private fun observeReceivedFriendRequestsRaw(): Flow<List<FriendRequest>> {
        val uid = requireUserId()
        return db.collection("friendRequests")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { it.toObjects(FriendRequest::class.java) }
    }

    fun observeReceivedFriendRequestsState(): Flow<UiDataState<List<FriendRequest>>> {
        return observeReceivedFriendRequestsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "friendRequests")
            friendlyFirestoreError(error, "friend requests")
        }
    }

    fun observeReceivedFriendRequests(): Flow<List<FriendRequest>> {
        val path = "friendRequests"
        return observeReceivedFriendRequestsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private fun observeSentFriendRequestsRaw(): Flow<List<FriendRequest>> {
        val uid = requireUserId()
        return db.collection("friendRequests")
            .whereEqualTo("senderId", uid)
            .whereEqualTo("status", "pending")
            .snapshots()
            .map { it.toObjects(FriendRequest::class.java) }
    }

    fun observeSentFriendRequestsState(): Flow<UiDataState<List<FriendRequest>>> {
        return observeSentFriendRequestsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "friendRequests")
            friendlyFirestoreError(error, "sent requests")
        }
    }

    fun observeSentFriendRequests(): Flow<List<FriendRequest>> {
        val path = "friendRequests"
        return observeSentFriendRequestsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendFriendRequest(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (uid == targetUserId) {
            return Result.failure(IllegalArgumentException("Cannot send a friend request to yourself."))
        }
        val sorted = listOf(uid, targetUserId).sorted()
        val friendshipId = "${sorted[0]}_${sorted[1]}"
        val requestId = "${uid}_$targetUserId"
        val reverseRequestId = "${targetUserId}_$uid"

        return try {
            val blockSnap = db.collection("blocks").document("${uid}_$targetUserId").get().await()
            if (blockSnap.exists()) {
                return Result.failure(IllegalStateException("Unblock this player before sending a friend request."))
            }

            val friendshipSnap = db.collection("friendships").document(friendshipId).get().await()
            if (friendshipSnap.exists()) {
                return Result.failure(IllegalStateException("You are already friends with this player."))
            }

            val reverseSnap = db.collection("friendRequests").document(reverseRequestId).get().await()
            if (reverseSnap.exists() && reverseSnap.getString("status") == "pending") {
                return Result.failure(IllegalStateException("This player already sent you a friend request. Check Requests!"))
            }

            val existingRef = db.collection("friendRequests").document(requestId)
            val existingSnap = existingRef.get().await()
            if (existingSnap.exists()) {
                val status = existingSnap.getString("status")
                if (status == "pending") {
                    return Result.failure(IllegalStateException("Friend request already sent."))
                }
                existingRef.update(
                    mapOf(
                        "status" to "pending",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val userDoc = db.collection("users").document(uid).get().await()
                val senderUsername = userDoc.getString("username") ?: "Gamer"
                val senderAvatarUrl = userDoc.getString("avatarUrl") ?: ""
                val payload = mapOf(
                    "senderId" to uid,
                    "senderUsername" to senderUsername,
                    "senderAvatarUrl" to senderAvatarUrl,
                    "receiverId" to targetUserId,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                existingRef.set(payload).await()
            }

            safetyRepository.createNotification(
                recipientUserId = targetUserId,
                title = "Friend Request",
                body = "Sent you a friend request on PRIME ESPORT.",
                type = "friend_request",
                targetId = uid,
                dedupId = "fr_${uid}_$targetUserId"
            )
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "friendRequests/$requestId")
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("friendRequests").document(requestId).update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "friendRequests/$requestId")
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String, senderId: String): Result<Unit> {
        val uid = requireUserId()
        val sortedUsers = listOf(uid, senderId).sorted()
        val friendshipId = "${sortedUsers[0]}_${sortedUsers[1]}"

        return try {
            db.runTransaction { tx ->
                val reqRef = db.collection("friendRequests").document(requestId)
                val friendshipRef = db.collection("friendships").document(friendshipId)
                val myUserRef = db.collection("users").document(uid)
                val otherUserRef = db.collection("users").document(senderId)

                val reqSnap = tx.get(reqRef)
                val friendshipSnap = tx.get(friendshipRef)
                val mySnap = tx.get(myUserRef)
                val otherSnap = tx.get(otherUserRef)

                if (!reqSnap.exists()) throw IllegalStateException("Friend request no longer exists.")

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "accepted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (!friendshipSnap.exists()) {
                    tx.set(
                        friendshipRef,
                        mapOf(
                            "user1" to sortedUsers[0],
                            "user2" to sortedUsers[1],
                            "users" to sortedUsers,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (mySnap.exists()) {
                        val myCount = (mySnap.getLong("friendsCount") ?: 0L).toInt()
                        tx.update(
                            myUserRef,
                            mapOf(
                                "friendsCount" to myCount + 1,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (otherSnap.exists()) {
                        val otherCount = (otherSnap.getLong("friendsCount") ?: 0L).toInt()
                        tx.update(
                            otherUserRef,
                            mapOf(
                                "friendsCount" to otherCount + 1,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()

            safetyRepository.createNotification(
                recipientUserId = senderId,
                title = "Friend Request Accepted",
                body = "Accepted your friend request! You can now chat.",
                type = "friend_accepted",
                targetId = uid
            )
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "friendRequests/$requestId")
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            db.collection("friendRequests").document(requestId).update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "friendRequests/$requestId")
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        val uid = requireUserId()
        val sortedUsers = listOf(uid, friendId).sorted()
        val friendshipId = "${sortedUsers[0]}_${sortedUsers[1]}"
        return try {
            db.runTransaction { tx ->
                val friendshipRef = db.collection("friendships").document(friendshipId)
                val myRef = db.collection("users").document(uid)
                val otherRef = db.collection("users").document(friendId)

                val friendshipSnap = tx.get(friendshipRef)
                val mySnap = tx.get(myRef)
                val otherSnap = tx.get(otherRef)

                if (friendshipSnap.exists()) {
                    tx.delete(friendshipRef)
                    if (mySnap.exists()) {
                        val c = (mySnap.getLong("friendsCount") ?: 0L).toInt()
                        tx.update(
                            myRef,
                            mapOf(
                                "friendsCount" to (c - 1).coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (otherSnap.exists()) {
                        val c = (otherSnap.getLong("friendsCount") ?: 0L).toInt()
                        tx.update(
                            otherRef,
                            mapOf(
                                "friendsCount" to (c - 1).coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "friendships/$friendshipId")
            Result.failure(e)
        }
    }

    private fun observeFriendshipsRaw(): Flow<List<Friendship>> {
        val uid = requireUserId()
        return db.collection("friendships")
            .whereArrayContains("users", uid)
            .snapshots()
            .map { it.toObjects(Friendship::class.java) }
    }

    fun observeFriendshipsState(): Flow<UiDataState<List<Friendship>>> {
        return observeFriendshipsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "friendships")
            friendlyFirestoreError(error, "friends")
        }
    }

    fun observeFriendships(): Flow<List<Friendship>> {
        val path = "friendships"
        return observeFriendshipsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    // --- Follows (Strictly Separate from Friends) ---
    fun observeFollowing(userId: String = requireUserId()): Flow<List<Follow>> {
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .snapshots()
            .map { it.toObjects(Follow::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "follows")
                emit(emptyList())
            }
    }

    fun observeFollowingState(userId: String = requireUserId()): Flow<UiDataState<List<Follow>>> {
        return db.collection("follows")
            .whereEqualTo("followerId", userId)
            .snapshots()
            .map { it.toObjects(Follow::class.java) }
            .asUiDataState { error ->
                handleFirestoreError(error, OperationType.LIST, "follows")
                friendlyFirestoreError(error, "following list")
            }
    }

    fun observeFollowers(userId: String): Flow<List<Follow>> {
        return db.collection("follows")
            .whereEqualTo("followingId", userId)
            .snapshots()
            .map { it.toObjects(Follow::class.java) }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "follows")
                emit(emptyList())
            }
    }

    fun observeFollowersState(userId: String): Flow<UiDataState<List<Follow>>> {
        return db.collection("follows")
            .whereEqualTo("followingId", userId)
            .snapshots()
            .map { it.toObjects(Follow::class.java) }
            .asUiDataState { error ->
                handleFirestoreError(error, OperationType.LIST, "follows")
                friendlyFirestoreError(error, "followers list")
            }
    }

    fun observeIsFollowing(targetUserId: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(false)
        if (uid == targetUserId) return kotlinx.coroutines.flow.flowOf(false)
        val followId = "${uid}_$targetUserId"
        return db.collection("follows").document(followId)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(false)
            }
    }

    suspend fun toggleFollow(targetUserId: String): Result<Boolean> {
        val uid = requireUserId()
        if (uid == targetUserId) {
            return Result.failure(IllegalArgumentException("You cannot follow yourself."))
        }
        val followId = "${uid}_$targetUserId"
        return try {
            val nowFollowing = db.runTransaction { tx ->
                val followRef = db.collection("follows").document(followId)
                val myRef = db.collection("users").document(uid)
                val targetRef = db.collection("users").document(targetUserId)

                val followSnap = tx.get(followRef)
                val mySnap = tx.get(myRef)
                val targetSnap = tx.get(targetRef)

                if (followSnap.exists()) {
                    tx.delete(followRef)
                    if (mySnap.exists()) {
                        val followingCount = (mySnap.getLong("followingCount") ?: 0L).toInt()
                        tx.update(
                            myRef,
                            mapOf(
                                "followingCount" to (followingCount - 1).coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (targetSnap.exists()) {
                        val followersCount = (targetSnap.getLong("followersCount") ?: 0L).toInt()
                        tx.update(
                            targetRef,
                            mapOf(
                                "followersCount" to (followersCount - 1).coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    false
                } else {
                    tx.set(
                        followRef,
                        mapOf(
                            "followerId" to uid,
                            "followingId" to targetUserId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (mySnap.exists()) {
                        val followingCount = (mySnap.getLong("followingCount") ?: 0L).toInt()
                        tx.update(
                            myRef,
                            mapOf(
                                "followingCount" to followingCount + 1,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    if (targetSnap.exists()) {
                        val followersCount = (targetSnap.getLong("followersCount") ?: 0L).toInt()
                        tx.update(
                            targetRef,
                            mapOf(
                                "followersCount" to followersCount + 1,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    true
                }
            }.await()

            if (nowFollowing) {
                safetyRepository.createNotification(
                    recipientUserId = targetUserId,
                    title = "New Follower",
                    body = "Started following your creator channel.",
                    type = "follow",
                    targetId = uid,
                    dedupId = "follow_${uid}_$targetUserId"
                )
            }
            Result.success(nowFollowing)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "follows/$followId")
            Result.failure(e)
        }
    }

    // --- Direct Messages (Friend-Gated) ---
    private fun observeConversationsRaw(): Flow<List<Conversation>> {
        val uid = requireUserId()
        return db.collection("conversations")
            .whereArrayContains("participants", uid)
            .snapshots()
            .map { it.toObjects(Conversation::class.java) }
    }

    fun observeConversationsState(): Flow<UiDataState<List<Conversation>>> {
        return observeConversationsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "conversations")
            friendlyFirestoreError(error, "conversations")
        }
    }

    fun observeConversations(): Flow<List<Conversation>> {
        val path = "conversations"
        return observeConversationsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun getOrCreateConversationWithFriend(friendId: String): Result<String> {
        val uid = requireUserId()
        val blockSnap = db.collection("blocks").document("${uid}_$friendId").get().await()
        if (blockSnap.exists()) {
            return Result.failure(IllegalStateException("Unblock this player before messaging them."))
        }

        val sortedUsers = listOf(uid, friendId).sorted()
        val friendshipId = "${sortedUsers[0]}_${sortedUsers[1]}"

        val friendship = db.collection("friendships").document(friendshipId).get().await()
        if (!friendship.exists()) {
            return Result.failure(IllegalStateException("Direct message requires an accepted friendship first."))
        }

        val conversationId = "conv_${sortedUsers[0]}_${sortedUsers[1]}"
        val convDoc = db.collection("conversations").document(conversationId).get().await()
        if (!convDoc.exists()) {
            db.collection("conversations").document(conversationId).set(
                mapOf(
                    "participants" to sortedUsers,
                    "lastMessage" to "Started conversation",
                    "lastSenderId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return Result.success(conversationId)
    }

    private fun observeMessagesRaw(conversationId: String): Flow<List<DirectMessage>> {
        return db.collection("conversations/$conversationId/messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(DirectMessage::class.java) }
    }

    fun observeMessagesState(conversationId: String): Flow<UiDataState<List<DirectMessage>>> {
        val path = "conversations/$conversationId/messages"
        return observeMessagesRaw(conversationId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, path)
            friendlyFirestoreError(error, "messages")
        }
    }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return observeMessagesRaw(conversationId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendDirectMessage(conversationId: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val messageId = UUID.randomUUID().toString()

        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val senderName = userDoc.getString("displayName") ?: userDoc.getString("username") ?: "Gamer"
            val convDoc = db.collection("conversations").document(conversationId).get().await()
            val participants = (convDoc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val recipientId = participants.firstOrNull { it != uid }

            val payload = mapOf(
                "conversationId" to conversationId,
                "senderId" to uid,
                "senderName" to senderName,
                "text" to cleanText,
                "mediaUrl" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("conversations").document(conversationId).collection("messages").document(messageId).set(payload).await()
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessage" to cleanText,
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (!recipientId.isNullOrBlank()) {
                safetyRepository.createNotification(
                    recipientUserId = recipientId,
                    title = "Message from $senderName",
                    body = cleanText.take(120),
                    type = "message",
                    targetId = conversationId
                )
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations/$conversationId/messages/$messageId")
            Result.failure(e)
        }
    }

    // --- Search Users ---
    fun searchUsers(query: String): Flow<List<UserProfile>> = flow {
        val q = query.trim().lowercase()
        val snap = db.collection("users").limit(100).get().await()
        val all = snap.toObjects(UserProfile::class.java)
        val filtered = if (q.isEmpty()) {
            all
        } else {
            all.filter {
                it.username.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.favoriteGames.lowercase().contains(q)
            }
        }
        emit(filtered)
    }.catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "users")
        emit(emptyList())
    }
}
