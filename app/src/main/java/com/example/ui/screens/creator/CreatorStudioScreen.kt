package com.example.ui.screens.creator

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelVideo
import com.example.data.model.UiDataState
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.SocialRepository
import com.example.data.storage.MediaUploadService
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.GamerAvatar
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.VerificationBadges
import com.example.ui.components.VideoPlayerModal
import com.example.ui.components.formatRelativeTime
import com.example.ui.screens.upload.CreateContentSheet
import com.example.ui.screens.upload.UploadType
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    creatorRepository: CreatorRepository,
    currentUserId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUploadService = remember(context) { MediaUploadService(context) }

    var retryTrigger by remember { mutableIntStateOf(0) }
    val userProfileState by remember(currentUserId, retryTrigger) {
        authRepository.observeUserProfileState(currentUserId)
    }.collectAsState(initial = UiDataState.Loading)
    val videosState by remember(currentUserId, retryTrigger) {
        creatorRepository.observeUserVideosState(currentUserId)
    }.collectAsState(initial = UiDataState.Loading)
    val userProfile = (userProfileState as? UiDataState.Success)?.data
    val videos = (videosState as? UiDataState.Success)?.data ?: emptyList()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Videos", "Analytics", "Channel Info")

    var showUploadVideoSheet by remember { mutableStateOf(false) }
    var playingVideo by remember { mutableStateOf<ChannelVideo?>(null) }
    var videoPendingDelete by remember { mutableStateOf<ChannelVideo?>(null) }
    var isDeletingVideo by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val displayName = userProfile?.displayName?.ifBlank { userProfile?.username ?: "Creator" } ?: "Creator"
    val username = userProfile?.username ?: ""

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                    }
                    Column {
                        Text("CREATOR STUDIO", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Your Profile • Your Channel", color = PrimeBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Unified Profile Channel Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimeNavySurface)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GamerAvatar(
                            avatarUrl = userProfile?.avatarUrl ?: "",
                            displayName = displayName,
                            size = 64
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    color = PrimeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                VerificationBadges(
                                    isOwner = userProfile?.isOwner == true,
                                    isVerified = userProfile?.isVerified == true,
                                    showOwnerPill = true
                                )
                            }
                            if (username.isNotBlank()) {
                                Text(
                                    text = "@$username",
                                    color = PrimeBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${userProfile?.followersCount ?: 0} followers • ${videos.size} videos",
                                color = PrimeTextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (!userProfile?.bio.isNullOrBlank()) {
                        Text(
                            text = userProfile?.bio ?: "",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showUploadVideoSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimeRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("upload_video_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrimeCardBackground,
                    contentColor = PrimeBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimeBlue,
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) PrimeBlue else PrimeTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    when (val currentVideosState = videosState) {
                        is UiDataState.Loading -> {
                            item {
                                PrimeLoadingBox(
                                    message = "Loading your studio videos...",
                                    accentColor = PrimeRed
                                )
                            }
                        }
                        is UiDataState.Error -> {
                            item {
                                PrimeErrorRetryCard(
                                    message = currentVideosState.message,
                                    accentColor = PrimeRed,
                                    onRetry = { retryTrigger++ }
                                )
                            }
                        }
                        is UiDataState.Success -> if (videos.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No videos uploaded yet.", color = PrimeTextMuted, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(videos, key = { it.id }) { video ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable {
                                        playingVideo = video
                                        coroutineScope.launch {
                                            creatorRepository.incrementVideoViews(video.id)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(175.dp)
                                            .background(PrimeNavySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (video.thumbnailUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = video.thumbnailUrl,
                                                contentDescription = video.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = video.title,
                                                color = PrimeTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            if (video.description.isNotBlank()) {
                                                Text(
                                                    text = video.description,
                                                    color = PrimeTextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("${video.viewsCount} views", color = PrimeTextMuted, fontSize = 11.sp)
                                                Text("•", color = PrimeTextMuted, fontSize = 11.sp)
                                                Text(video.category.ifBlank { "Gameplay" }, color = PrimeBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Text("•", color = PrimeTextMuted, fontSize = 11.sp)
                                                Text(formatRelativeTime(video.createdAt), color = PrimeTextMuted, fontSize = 11.sp)
                                            }
                                        }
                                        IconButton(onClick = {
                                            deleteError = null
                                            videoPendingDelete = video
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Video",
                                                tint = PrimeRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
                1 -> {
                    item {
                        val totalViews = videos.sumOf { it.viewsCount }
                        val avgViews = if (videos.isEmpty()) 0 else totalViews / videos.size
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Creator Studio Analytics", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnalyticsMetricCard(
                                    title = "Followers",
                                    value = "${userProfile?.followersCount ?: 0}",
                                    icon = Icons.Default.BarChart,
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsMetricCard(
                                    title = "Total Views",
                                    value = "$totalViews",
                                    icon = Icons.Default.Visibility,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnalyticsMetricCard(
                                    title = "Videos",
                                    value = "${videos.size}",
                                    icon = Icons.Default.VideoLibrary,
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsMetricCard(
                                    title = "Avg Views",
                                    value = "$avgViews",
                                    icon = Icons.Default.PlayArrow,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Channel Information", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your PRIME ESPORT profile is your creator channel. All videos, reels, and posts you publish appear on your public profile.",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (username.isNotBlank()) {
                                Text("Channel Handle: @$username", color = PrimeBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (!userProfile?.bio.isNullOrBlank()) {
                                Text(
                                    text = "Bio: ${userProfile?.bio}",
                                    color = PrimeTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            if (!userProfile?.favoriteGames.isNullOrBlank()) {
                                Text(
                                    text = "Games: ${userProfile?.favoriteGames}",
                                    color = PrimePurple,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUploadVideoSheet) {
        CreateContentSheet(
            socialRepository = socialRepository,
            creatorRepository = creatorRepository,
            currentUserId = currentUserId,
            initialMode = UploadType.VIDEO,
            onDismiss = { showUploadVideoSheet = false }
        )
    }

    playingVideo?.let { video ->
        VideoPlayerModal(
            video = video,
            creatorProfile = userProfile,
            isOwner = video.ownerId == currentUserId,
            onDismiss = { playingVideo = null },
            onOpenProfile = onOpenProfile,
            onDelete = {
                deleteError = null
                videoPendingDelete = video
            }
        )
    }

    videoPendingDelete?.let { targetVideo ->
        DeleteConfirmationDialog(
            title = "Delete Video",
            message = "Are you sure you want to permanently delete '${targetVideo.title}'?",
            isDeleting = isDeletingVideo,
            errorMessage = deleteError,
            onConfirm = {
                isDeletingVideo = true
                deleteError = null
                coroutineScope.launch {
                    val res = creatorRepository.deleteVideo(targetVideo, mediaUploadService)
                    isDeletingVideo = false
                    if (res.isSuccess) {
                        videoPendingDelete = null
                        if (playingVideo?.id == targetVideo.id) {
                            playingVideo = null
                        }
                    } else {
                        deleteError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete video."
                    }
                }
            },
            onDismiss = { videoPendingDelete = null }
        )
    }
}

@Composable
fun AnalyticsMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeNavySurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = PrimeTextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(title, color = PrimeTextMuted, fontSize = 12.sp)
        }
    }
}
