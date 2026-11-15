package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.BlockedUser
import com.example.data.model.NotificationItem
import com.example.data.model.UiDataState
import com.example.data.model.asUiDataState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SafetyRepository(
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

    private fun observeNotificationsRaw(): Flow<List<NotificationItem>> {
        val uid = requireUserId()
        return db.collection("notifications")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationItem::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeNotificationsState(): Flow<UiDataState<List<NotificationItem>>> {
        return observeNotificationsRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "notifications")
            friendlyFirestoreError(error, "notifications")
        }
    }

    fun observeNotifications(): Flow<List<NotificationItem>> {
        return observeNotificationsRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "notifications")
                emit(emptyList())
            }
    }

    // --- User Blocking ---
    fun observeBlockedUserIds(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptySet())
        return db.collection("blocks")
            .whereEqualTo("blockerId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BlockedUser::class.java)
                    .mapNotNull { it.blockedId.takeIf { id -> id.isNotBlank() } }
                    .toSet()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, "blocks")
                emit(emptySet())
            }
    }

    fun observeIsUserBlocked(targetUserId: String): Flow<Boolean> {
        val uid = auth.currentUser?.uid ?: return flowOf(false)
        if (targetUserId.isBlank() || targetUserId == uid) return flowOf(false)
        val blockId = "${uid}_$targetUserId"
        return db.collection("blocks").document(blockId)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.GET, "blocks/$blockId")
                emit(false)
            }
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (targetUserId.isBlank() || targetUserId == uid) {
            return Result.failure(IllegalArgumentException("Cannot block this account."))
        }
        val blockId = "${uid}_$targetUserId"
        val path = "blocks/$blockId"
        return try {
            db.collection("blocks").document(blockId).set(
                mapOf(
                    "blockerId" to uid,
                    "blockedId" to targetUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        if (targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid user ID."))
        }
        val blockId = "${uid}_$targetUserId"
        val path = "blocks/$blockId"
        return try {
            db.collection("blocks").document(blockId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        recipientUserId: String,
        title: String,
        body: String,
        type: String,
        targetId: String = "",
        dedupId: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (recipientUserId.isBlank() || recipientUserId == uid) return Result.success(Unit)

        val notificationId = (dedupId ?: UUID.randomUUID().toString())
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .take(120)

        return try {
            val senderDoc = db.collection("users").document(uid).get().await()
            val senderName = senderDoc.getString("username")
                ?: senderDoc.getString("displayName")
                ?: "Gamer"
            val senderAvatarUrl = senderDoc.getString("avatarUrl") ?: ""

            val payload = mapOf(
                "userId" to recipientUserId,
                "senderId" to uid,
                "senderName" to senderName.take(60),
                "senderAvatarUrl" to senderAvatarUrl,
                "title" to title.trim().take(120),
                "body" to body.trim().take(400),
                "type" to type,
                "targetId" to targetId.take(200),
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("notifications").document(notificationId)
            if (dedupId != null) {
                val alreadyExists = try {
                    docRef.get().await().exists()
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    true
                }
                if (!alreadyExists) {
                    docRef.set(payload).await()
                }
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications/$notificationId")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId).update("isRead", true).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(): Result<Unit> {
        val uid = requireUserId()
        return try {
            val unreadSnap = db.collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            if (unreadSnap.isEmpty) return Result.success(Unit)

            unreadSnap.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notificationId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "notifications/$notificationId")
            Result.failure(e)
        }
    }

    suspend fun submitReport(targetType: String, targetId: String, reason: String): Result<Unit> {
        val uid = requireUserId()
        val reportId = UUID.randomUUID().toString()
        val path = "reports/$reportId"
        return try {
            val payload = mapOf(
                "reporterId" to uid,
                "targetType" to targetType.trim(),
                "targetId" to targetId.trim(),
                "reason" to reason.trim(),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").document(reportId).set(payload).await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
