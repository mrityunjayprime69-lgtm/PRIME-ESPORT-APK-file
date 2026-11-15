package com.example.data.model

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface UiDataState<out T> {
    data object Loading : UiDataState<Nothing>
    data class Success<T>(val data: T) : UiDataState<T>
    data class Error(val message: String) : UiDataState<Nothing>
}

fun <T> Flow<T>.asUiDataState(
    onError: (Throwable) -> String
): Flow<UiDataState<T>> = this
    .map<T, UiDataState<T>> { UiDataState.Success(it) }
    .onStart { emit(UiDataState.Loading) }
    .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiDataState.Error(onError(firestoreError ?: error)))
    }
