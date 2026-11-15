package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }.toList()
    val firestoreException = causes.firstOrNull { it is FirebaseFirestoreException }
    if (firestoreException == null && causes.any { it is CancellationException }) {
        return ""
    }
    val rootError = firestoreException
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = Firebase.auth
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirestoreError(exception: Throwable, resourceLabel: String): String {
    val rootError = generateSequence(exception) { it.cause }
        .firstOrNull { it is FirebaseFirestoreException } as? FirebaseFirestoreException
    return when (rootError?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Permission denied while loading $resourceLabel."
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "Network unavailable while loading $resourceLabel. Check your connection and retry."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Your session expired. Please sign in again."
        else ->
            exception.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Unable to load $resourceLabel. Please try again."
    }
}
