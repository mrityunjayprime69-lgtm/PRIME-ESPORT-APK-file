package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class UploadProgress(
    val bytesTransferred: Long,
    val totalByteCount: Long,
    val percent: Int
)

data class UploadedMedia(
    val downloadUrl: String,
    val storagePath: String
)

class MediaUploadService(private val context: Context) {

    private val storage: FirebaseStorage? by lazy {
        try {
            Firebase.storage
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Uploads an image or video URI to Firebase Storage with live progress and cancellation support.
     * Never falls back to local-only paths so cloud media consistency is guaranteed.
     */
    suspend fun uploadMedia(
        uri: Uri,
        folder: String, // "avatars", "banners", "posts", "reels", "videos", "thumbnails"
        userId: String,
        onProgress: (UploadProgress) -> Unit = {},
        onTaskCreated: (UploadTask) -> Unit = {}
    ): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("You must be signed in to upload media."))
        }
        val fbStorage = storage ?: return@withContext Result.failure(
            IllegalStateException("Cloud Storage is not configured. Please check Firebase setup.")
        )

        val isVideoFolder = folder == "videos" || folder == "reels"
        val detectedMime = try {
            context.contentResolver.getType(uri)?.trim()?.lowercase()
        } catch (_: Exception) {
            null
        }

        if (isVideoFolder && !detectedMime.isNullOrBlank() && detectedMime != "application/octet-stream" && !detectedMime.startsWith("video/")) {
            return@withContext Result.failure(
                IllegalArgumentException("Please select a valid video file (MP4, WebM, or MKV).")
            )
        }
        if (!isVideoFolder && !detectedMime.isNullOrBlank() && detectedMime != "application/octet-stream" && !detectedMime.startsWith("image/")) {
            return@withContext Result.failure(
                IllegalArgumentException("Please select a valid image file (JPG, PNG, or WebP).")
            )
        }

        val safeName = getFileName(uri).replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val resolvedMimeType = resolveContentType(isVideoFolder, detectedMime, safeName)
        val normalizedFileName = ensureFileExtension(safeName, resolvedMimeType)
        val fileName = "${UUID.randomUUID()}_$normalizedFileName"
        val storagePath = "$folder/$userId/$fileName"
        val storageRef = fbStorage.reference.child(storagePath)
        val metadata = StorageMetadata.Builder()
            .setContentType(resolvedMimeType)
            .build()

        var uploadTask: UploadTask? = null
        return@withContext try {
            val isReadable = try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    afd.length != 0L
                } ?: false
            } catch (_: Exception) {
                true // Fallback to putFile if provider only exposes streaming InputStream
            }
            if (!isReadable) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selected media file is empty or unreadable.")
                )
            }

            uploadTask = storageRef.putFile(uri, metadata)
            onTaskCreated(uploadTask)
            uploadTask.addOnProgressListener { snapshot ->
                val total = snapshot.totalByteCount
                val transferred = snapshot.bytesTransferred
                val percent = if (total > 0) ((transferred * 100) / total).toInt().coerceIn(0, 100) else 0
                onProgress(UploadProgress(transferred, total, percent))
            }

            uploadTask.await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            onProgress(UploadProgress(1, 1, 100))
            Result.success(UploadedMedia(downloadUrl = downloadUrl, storagePath = storagePath))
        } catch (ce: CancellationException) {
            try {
                uploadTask?.cancel()
            } catch (_: Exception) {}
            throw ce
        } catch (e: Exception) {
            val friendlyMessage = when (e) {
                is StorageException -> when (e.errorCode) {
                    StorageException.ERROR_NOT_AUTHENTICATED,
                    StorageException.ERROR_NOT_AUTHORIZED -> "Permission denied while uploading to cloud storage."
                    StorageException.ERROR_CANCELED -> "Upload was cancelled."
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Network timeout during upload. Please try again."
                    else -> e.localizedMessage ?: "Cloud storage upload failed."
                }
                else -> e.localizedMessage ?: "Cloud storage upload failed."
            }
            Result.failure(Exception(friendlyMessage, e))
        }
    }

    private fun resolveContentType(isVideo: Boolean, detectedMime: String?, fileName: String): String {
        if (!detectedMime.isNullOrBlank() && detectedMime != "application/octet-stream") {
            if (isVideo && detectedMime.startsWith("video/")) return detectedMime
            if (!isVideo && detectedMime.startsWith("image/")) return detectedMime
        }
        val lower = fileName.lowercase()
        return if (isVideo) {
            when {
                lower.endsWith(".webm") -> "video/webm"
                lower.endsWith(".mkv") -> "video/x-matroska"
                lower.endsWith(".mov") -> "video/quicktime"
                lower.endsWith(".3gp") -> "video/3gpp"
                else -> "video/mp4"
            }
        } else {
            when {
                lower.endsWith(".png") -> "image/png"
                lower.endsWith(".webp") -> "image/webp"
                lower.endsWith(".gif") -> "image/gif"
                else -> "image/jpeg"
            }
        }
    }

    private fun ensureFileExtension(fileName: String, contentType: String): String {
        if (fileName.contains('.')) return fileName
        val ext = when (contentType) {
            "video/webm" -> ".webm"
            "video/x-matroska" -> ".mkv"
            "video/quicktime" -> ".mov"
            "video/3gpp" -> ".3gp"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            else -> if (contentType.startsWith("video/")) ".mp4" else ".jpg"
        }
        return "$fileName$ext"
    }

    /**
     * Deletes previously uploaded cloud media safely when a post, reel, video, or avatar is removed.
     */
    suspend fun deleteMedia(storagePath: String, fallbackUrl: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        val fbStorage = storage ?: return@withContext Result.success(Unit)
        return@withContext try {
            val ref = when {
                storagePath.isNotBlank() -> fbStorage.reference.child(storagePath)
                fallbackUrl.startsWith("https://firebasestorage.") || fallbackUrl.startsWith("gs://") ->
                    fbStorage.getReferenceFromUrl(fallbackUrl)
                else -> null
            }
            if (ref != null) {
                ref.delete().await()
            }
            Result.success(Unit)
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "media_${System.currentTimeMillis()}"
        try {
            if (uri.scheme == "file") {
                uri.lastPathSegment?.let { if (it.isNotBlank()) name = it }
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                }
            }
        } catch (_: Exception) {}
        return name.replace(" ", "_")
    }
}
