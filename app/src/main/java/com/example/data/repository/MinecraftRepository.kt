package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.friendlyFirestoreError
import com.example.data.handleFirestoreError
import com.example.data.model.MinecraftServer
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MinecraftRepository(
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

    private fun observeMinecraftServersRaw(): Flow<List<MinecraftServer>> {
        return db.collection("minecraftServers")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MinecraftServer::class.java)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
    }

    fun observeMinecraftServersState(): Flow<UiDataState<List<MinecraftServer>>> {
        return observeMinecraftServersRaw().asUiDataState { error ->
            handleFirestoreError(error, OperationType.LIST, "minecraftServers")
            friendlyFirestoreError(error, "Minecraft servers")
        }
    }

    fun observeMinecraftServers(): Flow<List<MinecraftServer>> {
        val path = "minecraftServers"
        return observeMinecraftServersRaw()
            .catch { error ->
                if (error is CancellationException) throw error
                handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun addMinecraftServer(
        name: String,
        edition: String,
        version: String,
        address: String,
        port: Int,
        description: String
    ): Result<String> {
        val uid = requireUserId()
        val serverId = UUID.randomUUID().toString()
        val path = "minecraftServers/$serverId"
        return try {
            val payload = mapOf(
                "ownerId" to uid,
                "name" to name.trim(),
                "edition" to edition.trim(),
                "version" to version.trim(),
                "address" to address.trim(),
                "port" to port,
                "description" to description.trim(),
                "onlinePlayers" to 0,
                "maxPlayers" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("minecraftServers").document(serverId).set(payload).await()
            Result.success(serverId)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMinecraftServer(serverId: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val snap = db.collection("minecraftServers").document(serverId).get().await()
            if (snap.getString("ownerId") != uid) {
                return Result.failure(SecurityException("Only the server owner can delete this listing."))
            }
            db.collection("minecraftServers").document(serverId).delete().await()
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "minecraftServers/$serverId")
            Result.failure(e)
        }
    }
}
