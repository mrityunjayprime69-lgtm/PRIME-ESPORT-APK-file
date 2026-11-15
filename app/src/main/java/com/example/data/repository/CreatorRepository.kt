package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.ChannelVideo
import com.example.data.model.UiDataState
import com.example.data.model.asUiDataState
import com.example.data.storage.MediaUploadService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages creator videos belonging directly to a user's unified PRIME ESPORT profile channel.
 */
class CreatorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isPlayableRemoteVideo(video: ChannelVideo): Boolean {
        val url = video.videoUrl.trim()
        val isHttp = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        return isHttp && !EsportRepository.isFakeOrSystemId(video.id) && !EsportRepository.isFakeOrSystemId(video.ownerId)
    }

    private fun observeAllPublishedVideosRaw(): Flow<List<ChannelVideo>> {
        return db.collection("channelVideos")
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChannelVideo::class.java)
                    .filter(::isPlayableRemoteVideo)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeAllPublishedVideosState(): Flow<UiDataState<List<ChannelVideo>>> {
        return observeAllPublishedVideosRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "channelVideos")
            friendlyFirestoreError(error, "videos")
        }
    }

    fun observeAllPublishedVideos(): Flow<List<ChannelVideo>> {
        val path = "channelVideos"
        return observeAllPublishedVideosRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    private fun observeUserVideosRaw(userId: String): Flow<List<ChannelVideo>> {
        return db.collection("channelVideos")
            .whereEqualTo("ownerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChannelVideo::class.java)
                    .filter { it.status == "published" && isPlayableRemoteVideo(it) }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeUserVideosState(userId: String): Flow<UiDataState<List<ChannelVideo>>> {
        return observeUserVideosRaw(userId).asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "channelVideos")
            friendlyFirestoreError(error, "videos")
        }
    }

    fun observeUserVideos(userId: String): Flow<List<ChannelVideo>> {
        val path = "channelVideos"
        return observeUserVideosRaw(userId)
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun uploadVideo(
        title: String,
        videoUrl: String,
        description: String,
        category: String,
        thumbnailUrl: String = "",
        storagePath: String = "",
        thumbnailStoragePath: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val videoId = UUID.randomUUID().toString()
        val path = "channelVideos/$videoId"
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "Gamer"
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            val payload = mapOf(
                "channelId" to uid,
                "ownerId" to uid,
                "authorUsername" to username,
                "authorAvatarUrl" to avatarUrl,
                "title" to title.trim(),
                "videoUrl" to videoUrl.trim(),
                "storagePath" to storagePath.trim(),
                "description" to description.trim(),
                "thumbnailUrl" to thumbnailUrl.trim(),
                "thumbnailStoragePath" to thumbnailStoragePath.trim(),
                "viewsCount" to 0,
                "category" to category.trim(),
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("channelVideos").document(videoId).set(payload).await()
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "videosCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {}
            Result.success(videoId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVideo(
        video: ChannelVideo,
        mediaUploadService: MediaUploadService? = null
    ): Result<Unit> {
        val uid = requireUserId()
        if (video.ownerId != uid) {
            return Result.failure(SecurityException("Only the owner can delete this video."))
        }
        return try {
            db.runTransaction { tx ->
                val videoRef = db.collection("channelVideos").document(video.id)
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val currentCount = (userSnap.getLong("videosCount") ?: 0L).toInt()
                tx.delete(videoRef)
                if (userSnap.exists()) {
                    tx.update(
                        userRef,
                        mapOf(
                            "videosCount" to (currentCount - 1).coerceAtLeast(0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            if (mediaUploadService != null) {
                if (video.storagePath.isNotBlank() || video.videoUrl.isNotBlank()) {
                    mediaUploadService.deleteMedia(video.storagePath, video.videoUrl)
                }
                if (video.thumbnailStoragePath.isNotBlank() || video.thumbnailUrl.isNotBlank()) {
                    mediaUploadService.deleteMedia(video.thumbnailStoragePath, video.thumbnailUrl)
                }
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "channelVideos/${video.id}")
            Result.failure(e)
        }
    }

    suspend fun incrementVideoViews(videoId: String) {
        try {
            db.collection("channelVideos").document(videoId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1)
                )
            ).await()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {}
    }
}
