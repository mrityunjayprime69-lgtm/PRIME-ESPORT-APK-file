package com.example.ui.screens.reels

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Reel
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.preferences.DataSaverSettings
import com.example.data.preferences.DataStoreManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.data.storage.MediaUploadService
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.GamerAvatar
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.PrimeVideoPlayer
import com.example.ui.components.ReportContentSheet
import com.example.ui.components.VerificationBadges
import com.example.ui.components.formatRelativeTime
import com.example.ui.screens.upload.UploadType
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    safetyRepository: SafetyRepository,
    dataStoreManager: DataStoreManager,
    currentUserId: String,
    onOpenProfile: (String) -> Unit = {},
    onOpenUploadSheet: (UploadType) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUploadService = remember(context) { MediaUploadService(context) }

    var retryTrigger by remember { mutableIntStateOf(0) }
    val reelsState by remember(retryTrigger) {
        socialRepository.observeReelsState()
    }.collectAsState(initial = UiDataState.Loading)
    val blockedUserIds by safetyRepository.observeBlockedUserIds().collectAsState(initial = emptySet())
    val rawReels = (reelsState as? UiDataState.Success)?.data ?: emptyList()
    val reels = remember(rawReels, blockedUserIds) {
        rawReels.filter { !blockedUserIds.contains(it.authorId) }
    }
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())
    val dataSaverSettings by dataStoreManager.dataSaverSettings.collectAsState(initial = DataSaverSettings())
    val savedReelIds by dataStoreManager.savedReelIds.collectAsState(initial = emptySet())

    var selectedReelForComments by remember { mutableStateOf<Reel?>(null) }
    var selectedReelForReport by remember { mutableStateOf<Reel?>(null) }
    var reelPendingDelete by remember { mutableStateOf<Reel?>(null) }
    var isDeletingReel by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenUploadSheet(UploadType.REEL) },
                containerColor = PrimeRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("upload_reel_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Reel")
            }
        }
    ) { innerPadding ->
        when (val currentReelsState = reelsState) {
            is UiDataState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(PrimeDarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    PrimeLoadingBox(
                        message = "Loading reels...",
                        accentColor = PrimeRed
                    )
                }
            }
            is UiDataState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(PrimeDarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    PrimeErrorRetryCard(
                        message = currentReelsState.message,
                        accentColor = PrimeRed,
                        onRetry = { retryTrigger++ }
                    )
                }
            }
            is UiDataState.Success -> if (reels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(PrimeDarkBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎬", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Reels available yet.",
                            color = PrimeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Upload a gaming clip from your device to get featured here.",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        Button(
                            onClick = { onOpenUploadSheet(UploadType.REEL) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimeRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Upload Reel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            else -> {
                val pagerState = rememberPagerState(pageCount = { reels.size })
                val activeReel = reels.getOrNull(pagerState.currentPage)
                LaunchedEffect(activeReel?.id) {
                    val rid = activeReel?.id
                    if (!rid.isNullOrBlank()) {
                        socialRepository.incrementReelViews(rid)
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { page ->
                    val reel = reels[page]
                    val isLiked by socialRepository.observeIsReelLiked(reel.id).collectAsState(initial = false)
                    val authorProfile = usersMap[reel.authorId]
                    val isSaved = savedReelIds.contains(reel.id)
                    val isOwner = reel.authorId == currentUserId

                    ReelItem(
                        reel = reel,
                        authorProfile = authorProfile,
                        isCurrentPage = pagerState.currentPage == page,
                        dataSaverAutoPlay = dataSaverSettings.autoPlayReels,
                        isLiked = isLiked,
                        isSaved = isSaved,
                        isOwner = isOwner,
                        onLike = {
                            coroutineScope.launch {
                                socialRepository.toggleLikeReel(reel.id)
                            }
                        },
                        onComments = { selectedReelForComments = reel },
                        onToggleSave = {
                            coroutineScope.launch {
                                dataStoreManager.toggleSavedReel(reel.id)
                            }
                        },
                        onShare = {
                            val handle = authorProfile?.username ?: reel.authorUsername
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch this gaming reel by @$handle on PRIME ESPORT: ${reel.caption}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                        },
                        onReport = { selectedReelForReport = reel },
                        onDelete = {
                            deleteError = null
                            reelPendingDelete = reel
                        },
                        onAuthorClick = {
                            if (reel.authorId.isNotBlank()) onOpenProfile(reel.authorId)
                        }
                    )
                }
            }
        }
    }

    // Reel Comments Bottom Sheet
    selectedReelForComments?.let { reel ->
        ReelCommentsSheet(
            reel = reel,
            usersMap = usersMap,
            socialRepository = socialRepository,
            blockedUserIds = blockedUserIds,
            onOpenProfile = onOpenProfile,
            onDismiss = { selectedReelForComments = null }
        )
    }

    // Report Reel Sheet
    selectedReelForReport?.let { reel ->
        ReportContentSheet(
            title = "Report Reel",
            targetType = "reel",
            targetId = reel.id,
            safetyRepository = safetyRepository,
            onDismiss = { selectedReelForReport = null }
        )
    }

    // Owner-only Reel Deletion Dialog
    reelPendingDelete?.let { reel ->
        DeleteConfirmationDialog(
            title = "Delete Reel",
            message = "Are you sure you want to permanently delete this Reel?",
            isDeleting = isDeletingReel,
            errorMessage = deleteError,
            onConfirm = {
                isDeletingReel = true
                deleteError = null
                coroutineScope.launch {
                    val res = socialRepository.deleteReel(reel, mediaUploadService)
                    isDeletingReel = false
                    if (res.isSuccess) {
                        reelPendingDelete = null
                    } else {
                        deleteError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete Reel."
                    }
                }
            },
            onDismiss = { reelPendingDelete = null }
        )
    }
}

@Composable
fun ReelItem(
    reel: Reel,
    authorProfile: UserProfile?,
    isCurrentPage: Boolean,
    dataSaverAutoPlay: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    isOwner: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
    onAuthorClick: () -> Unit
) {
    var userPaused by remember(reel.id) { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    val shouldPlay = isCurrentPage && dataSaverAutoPlay && !userPaused

    val displayName = authorProfile?.displayName?.ifBlank { authorProfile.username }
        ?: reel.authorUsername.ifBlank { "Creator" }
    val handle = authorProfile?.username?.ifBlank { null } ?: reel.authorUsername
    val avatarUrl = authorProfile?.avatarUrl?.ifBlank { null } ?: reel.authorAvatarUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { userPaused = !userPaused }
    ) {
        // Real Media3 ExoPlayer Reel Playback
        PrimeVideoPlayer(
            videoUrl = reel.videoUrl,
            autoPlay = shouldPlay,
            isMuted = isMuted,
            showController = false,
            loop = true,
            resizeModeZoom = true,
            thumbnailUrl = reel.thumbnailUrl,
            isActive = isCurrentPage,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom readability scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.2f),
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )

        // Play indicator when paused
        if (!shouldPlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Reel",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Mute toggle top-right
        IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Overlay Content & Right Action Rail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 14.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Author & Caption
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAuthorClick() }
                ) {
                    GamerAvatar(
                        avatarUrl = avatarUrl,
                        displayName = displayName,
                        size = 40
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (handle.isNotBlank()) "@$handle" else displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            VerificationBadges(
                                isOwner = authorProfile?.isOwner == true,
                                isVerified = authorProfile?.isVerified == true
                            )
                        }
                        Text(
                            text = "${reel.viewsCount} views • ${formatRelativeTime(reel.createdAt)}",
                            color = PrimeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = reel.caption,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (reel.tags.isNotBlank()) {
                    Text(
                        text = reel.tags,
                        color = PrimeBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Right Actions Rail
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Like
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Reel",
                            tint = if (isLiked) PrimeRed else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${reel.likesCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comments
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onComments,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Reel Comments",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "${reel.commentsCount}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bookmark / Save (Persisted)
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Reel",
                        tint = if (isSaved) PrimeBlue else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Share
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Reel",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Delete (Owner only) or Report
                if (isOwner) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Reel",
                            tint = PrimeRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report Reel",
                            tint = PrimeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelCommentsSheet(
    reel: Reel,
    usersMap: Map<String, UserProfile>,
    socialRepository: SocialRepository,
    blockedUserIds: Set<String> = emptySet(),
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by socialRepository.observeReelComments(reel.id).collectAsState(initial = emptyList())
    val visibleComments = remember(comments, blockedUserIds) {
        comments.filter { !blockedUserIds.contains(it.authorId) }
    }
    var commentText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Comments (${visibleComments.size})",
                color = PrimeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(300.dp)
            ) {
                if (visibleComments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Be the first to comment!",
                            color = PrimeTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(visibleComments, key = { it.id }) { comment ->
                        val profile = usersMap[comment.authorId]
                        val name = profile?.username?.ifBlank { null } ?: comment.authorUsername
                        val avatar = profile?.avatarUrl?.ifBlank { null } ?: comment.authorAvatarUrl
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            GamerAvatar(
                                avatarUrl = avatar,
                                displayName = name,
                                size = 32,
                                onClick = {
                                    onDismiss()
                                    onOpenProfile(comment.authorId)
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        onDismiss()
                                        onOpenProfile(comment.authorId)
                                    }
                                ) {
                                    Text(name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    VerificationBadges(
                                        isOwner = profile?.isOwner == true,
                                        isVerified = profile?.isVerified == true
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(formatRelativeTime(comment.createdAt), color = PrimeTextMuted, fontSize = 11.sp)
                                }
                                Text(comment.text, color = PrimeTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeRed,
                        unfocusedBorderColor = PrimeBorder
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            isSending = true
                            coroutineScope.launch {
                                val res = socialRepository.addReelComment(reel.id, commentText)
                                isSending = false
                                if (res.isSuccess) {
                                    commentText = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to comment.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = commentText.isNotBlank() && !isSending
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment", tint = PrimeRed)
                }
            }
        }
    }
}
