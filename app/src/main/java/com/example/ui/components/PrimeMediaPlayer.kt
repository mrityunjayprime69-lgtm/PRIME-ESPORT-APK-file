package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.ChannelVideo
import com.example.data.model.UserProfile
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary

private val NON_STREAMABLE_WEB_HOSTS = setOf(
    "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be",
    "drive.google.com", "instagram.com", "www.instagram.com",
    "tiktok.com", "www.tiktok.com", "example.com", "www.example.com", "example.org"
)

private val IMAGE_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")

fun validatePlayableVideoSource(rawUrl: String, allowLocalUri: Boolean): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return "Video source is missing."
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return "Invalid video link."
    val scheme = uri.scheme?.lowercase() ?: ""
    return when (scheme) {
        "http", "https" -> {
            val host = uri.host?.lowercase() ?: ""
            if (host.isBlank()) {
                "Invalid video URL."
            } else if (host in NON_STREAMABLE_WEB_HOSTS) {
                "This link is a webpage and cannot be streamed directly."
            } else {
                val pathLower = uri.path?.lowercase() ?: ""
                if (IMAGE_EXTENSIONS.any { pathLower.endsWith(it) }) {
                    "Image files cannot be played as video."
                } else {
                    null
                }
            }
        }
        "content", "file" -> {
            if (allowLocalUri) null else "This local video is no longer accessible."
        }
        else -> "Unsupported video link."
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PrimeVideoPlayer(
    videoUrl: String,
    autoPlay: Boolean = true,
    isMuted: Boolean = false,
    showController: Boolean = true,
    loop: Boolean = false,
    resizeModeZoom: Boolean = false,
    thumbnailUrl: String = "",
    allowLocalUri: Boolean = false,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    onFirstFrame: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preValidationError = remember(videoUrl, allowLocalUri) {
        validatePlayableVideoSource(videoUrl, allowLocalUri)
    }

    var retryKey by remember(videoUrl) { mutableIntStateOf(0) }
    var isBuffering by remember(videoUrl, retryKey) { mutableStateOf(preValidationError == null && isActive) }
    var hasFirstFrame by remember(videoUrl, retryKey) { mutableStateOf(false) }
    var playbackError by remember(videoUrl, retryKey) { mutableStateOf(preValidationError) }

    val exoPlayer = remember(videoUrl, retryKey, preValidationError) {
        if (preValidationError != null) {
            null
        } else {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            val uri = Uri.parse(videoUrl.trim())
            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (uri.path?.lowercase()?.endsWith(".m3u8") == true) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    volume = if (isMuted) 0f else 1f
                    playWhenReady = autoPlay && isActive
                    setMediaItem(mediaItemBuilder.build())
                    if (isActive) {
                        prepare()
                    }
                }
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer, autoPlay, isActive, playbackError) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (playbackError != null) return@LaunchedEffect
        if (isActive) {
            if (player.playbackState == Player.STATE_IDLE) {
                isBuffering = true
                player.prepare()
            }
            player.playWhenReady = autoPlay
        } else {
            player.playWhenReady = false
            player.pause()
        }
    }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING && isActive
                    if (playbackState == Player.STATE_READY) {
                        playbackError = null
                        isBuffering = false
                    }
                }

                override fun onRenderedFirstFrame() {
                    hasFirstFrame = true
                    isBuffering = false
                    onFirstFrame()
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    hasFirstFrame = false
                    playbackError = when {
                        error.cause is UnrecognizedInputFormatException ||
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                            "This video format is unsupported or the source cannot be played."
                        error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            "Video file is unavailable on the server."
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Network error while loading video. Tap Retry."
                        else -> error.localizedMessage ?: "Unable to play this video."
                    }
                }
            }
            exoPlayer.addListener(listener)

            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> if (isActive && autoPlay && playbackError == null) exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = showController
                        resizeMode = if (resizeModeZoom) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.useController = showController
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!hasFirstFrame && thumbnailUrl.isNotBlank() && playbackError == null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = if (resizeModeZoom) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = PrimeBlue,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }

        if (playbackError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Playback error",
                    tint = PrimeRed,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playbackError ?: "Video playback failed",
                    color = PrimeTextPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                if (preValidationError == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { retryKey++ },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerModal(
    video: ChannelVideo,
    creatorProfile: UserProfile?,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val creatorName = creatorProfile?.displayName?.ifBlank { creatorProfile.username }
        ?: video.authorUsername.ifBlank { "Creator" }
    val creatorHandle = creatorProfile?.username?.ifBlank { null } ?: video.authorUsername
    val creatorAvatar = creatorProfile?.avatarUrl?.ifBlank { null } ?: video.authorAvatarUrl

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = PrimeDarkBackground,
            modifier = Modifier
                .fillMaxSize()
                .testTag("video_player_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.category.ifBlank { "PRIME VIDEO" },
                        color = PrimePurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOwner && onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Video",
                                    tint = PrimeRed
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_video_player")) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Player",
                                tint = PrimeTextPrimary
                            )
                        }
                    }
                }

                PrimeVideoPlayer(
                    videoUrl = video.videoUrl,
                    autoPlay = true,
                    showController = true,
                    loop = false,
                    thumbnailUrl = video.thumbnailUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = video.title,
                        color = PrimeTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${video.viewsCount} views • ${formatRelativeTime(video.createdAt)}",
                        color = PrimeTextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PrimeBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimeCardBackground)
                            .clickable {
                                onDismiss()
                                onOpenProfile(video.ownerId)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GamerAvatar(avatarUrl = creatorAvatar, displayName = creatorName, size = 44)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = creatorName,
                                    color = PrimeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                VerificationBadges(
                                    isOwner = creatorProfile?.isOwner == true,
                                    isVerified = creatorProfile?.isVerified == true
                                )
                            }
                            if (creatorHandle.isNotBlank()) {
                                Text(
                                    text = "@$creatorHandle • View Channel",
                                    color = PrimeBlue,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (video.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = PrimeNavySurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Description",
                                    color = PrimeTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = video.description,
                                    color = PrimeTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
