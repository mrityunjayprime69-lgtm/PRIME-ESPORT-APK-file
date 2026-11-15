package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prime_esport_preferences")

data class DataSaverSettings(
    val masterDataSaver: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val autoPlayReels: Boolean = true,
    val videoQuality: String = "Auto", // Auto, Low, Medium, High
    val imageQuality: String = "Standard", // Standard, High
    val preloadVideos: Boolean = true,
    val preloadImages: Boolean = true,
    val downloadWifiOnly: Boolean = false,
    val uploadWifiOnly: Boolean = false,
    val backgroundData: Boolean = true,
    val autoDownloadMedia: Boolean = true,
    val reduceAnimations: Boolean = false
)

data class AppLanguageSettings(
    val languageCode: String = "en" // en, hi
)

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_MASTER_SAVER = booleanPreferencesKey("master_data_saver")
        private val KEY_AUTOPLAY_VIDEOS = booleanPreferencesKey("autoplay_videos")
        private val KEY_AUTOPLAY_REELS = booleanPreferencesKey("autoplay_reels")
        private val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val KEY_IMAGE_QUALITY = stringPreferencesKey("image_quality")
        private val KEY_PRELOAD_VIDEOS = booleanPreferencesKey("preload_videos")
        private val KEY_PRELOAD_IMAGES = booleanPreferencesKey("preload_images")
        private val KEY_WIFI_DOWNLOAD_ONLY = booleanPreferencesKey("download_wifi_only")
        private val KEY_WIFI_UPLOAD_ONLY = booleanPreferencesKey("upload_wifi_only")
        private val KEY_BACKGROUND_DATA = booleanPreferencesKey("background_data")
        private val KEY_AUTO_DOWNLOAD_MEDIA = booleanPreferencesKey("auto_download_media")
        private val KEY_REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_SAVED_REELS = androidx.datastore.preferences.core.stringSetPreferencesKey("saved_reel_ids")
    }

    val dataSaverSettings: Flow<DataSaverSettings> = context.dataStore.data.map { prefs ->
        val master = prefs[KEY_MASTER_SAVER] ?: false
        if (master) {
            DataSaverSettings(
                masterDataSaver = true,
                autoPlayVideos = false,
                autoPlayReels = false,
                videoQuality = "Low",
                imageQuality = "Standard",
                preloadVideos = false,
                preloadImages = false,
                downloadWifiOnly = true,
                uploadWifiOnly = true,
                backgroundData = false,
                autoDownloadMedia = false,
                reduceAnimations = true
            )
        } else {
            DataSaverSettings(
                masterDataSaver = false,
                autoPlayVideos = prefs[KEY_AUTOPLAY_VIDEOS] ?: true,
                autoPlayReels = prefs[KEY_AUTOPLAY_REELS] ?: true,
                videoQuality = prefs[KEY_VIDEO_QUALITY] ?: "Auto",
                imageQuality = prefs[KEY_IMAGE_QUALITY] ?: "Standard",
                preloadVideos = prefs[KEY_PRELOAD_VIDEOS] ?: true,
                preloadImages = prefs[KEY_PRELOAD_IMAGES] ?: true,
                downloadWifiOnly = prefs[KEY_WIFI_DOWNLOAD_ONLY] ?: false,
                uploadWifiOnly = prefs[KEY_WIFI_UPLOAD_ONLY] ?: false,
                backgroundData = prefs[KEY_BACKGROUND_DATA] ?: true,
                autoDownloadMedia = prefs[KEY_AUTO_DOWNLOAD_MEDIA] ?: true,
                reduceAnimations = prefs[KEY_REDUCE_ANIMATIONS] ?: false
            )
        }
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en"
    }

    val savedReelIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_REELS] ?: emptySet()
    }

    suspend fun toggleSavedReel(reelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_REELS] ?: emptySet()
            prefs[KEY_SAVED_REELS] = if (current.contains(reelId)) current - reelId else current + reelId
        }
    }

    suspend fun setMasterDataSaver(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MASTER_SAVER] = enabled
        }
    }

    suspend fun setAutoPlayVideos(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY_VIDEOS] = enabled
        }
    }

    suspend fun setAutoPlayReels(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY_REELS] = enabled
        }
    }

    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIDEO_QUALITY] = quality
        }
    }

    suspend fun setImageQuality(quality: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IMAGE_QUALITY] = quality
        }
    }

    suspend fun setPreloadVideos(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRELOAD_VIDEOS] = enabled
        }
    }

    suspend fun setPreloadImages(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRELOAD_IMAGES] = enabled
        }
    }

    suspend fun setDownloadWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_DOWNLOAD_ONLY] = enabled
        }
    }

    suspend fun setUploadWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_UPLOAD_ONLY] = enabled
        }
    }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = languageCode
        }
    }

    fun clearCache(): Long {
        return try {
            val cacheDir = context.cacheDir
            val size = getFolderSize(cacheDir)
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            size
        } catch (e: Exception) {
            0L
        }
    }

    fun getCacheSize(): Long {
        return try {
            getFolderSize(context.cacheDir)
        } catch (e: Exception) {
            0L
        }
    }

    private fun getFolderSize(file: java.io.File): Long {
        var size = 0L
        file.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }
}
