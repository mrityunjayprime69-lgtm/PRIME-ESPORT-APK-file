package com.example.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelVideo
import com.example.data.model.Post
import com.example.data.model.UiDataState
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.data.storage.MediaUploadService
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.PostCard
import com.example.ui.components.PostCommentsSheet
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.ReportContentSheet
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.upload.UploadType
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    safetyRepository: SafetyRepository,
    creatorRepository: CreatorRepository,
    currentUserId: String,
    isHindi: Boolean = false,
    onOpenProfile: (String) -> Unit = {},
    onOpenUploadSheet: (UploadType) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUploadService = remember(context) { MediaUploadService(context) }

    var retryTrigger by remember { mutableIntStateOf(0) }
    val postsState by remember(retryTrigger) {
        socialRepository.observePostsState()
    }.collectAsState(initial = UiDataState.Loading)
    val videosState by remember(retryTrigger) {
        creatorRepository.observeAllPublishedVideosState()
    }.collectAsState(initial = UiDataState.Loading)
    val blockedUserIds by safetyRepository.observeBlockedUserIds().collectAsState(initial = emptySet())

    val rawPosts = (postsState as? UiDataState.Success)?.data ?: emptyList()
    val posts = remember(rawPosts, blockedUserIds) {
        rawPosts.filter { !blockedUserIds.contains(it.authorId) }
    }
    val rawVideos = (videosState as? UiDataState.Success)?.data ?: emptyList()
    val videos = remember(rawVideos, blockedUserIds) {
        rawVideos.filter { !blockedUserIds.contains(it.ownerId) }
    }
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())
    val following by socialRepository.observeFollowing(currentUserId).collectAsState(initial = emptyList())
    val followingIds = remember(following) { following.map { it.followingId }.toSet() }

    var selectedCategory by remember { mutableStateOf("For You") }
    val categories = listOf("For You", "Following", "BGMI", "Minecraft", "Free Fire", "Valorant")

    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    var selectedPostForReport by remember { mutableStateOf<Post?>(null) }
    var postPendingDelete by remember { mutableStateOf<Post?>(null) }
    var isDeletingPost by remember { mutableStateOf(false) }
    var deletePostError by remember { mutableStateOf<String?>(null) }

    var playingVideo by remember { mutableStateOf<ChannelVideo?>(null) }
    var videoPendingDelete by remember { mutableStateOf<ChannelVideo?>(null) }
    var isDeletingVideo by remember { mutableStateOf(false) }
    var deleteVideoError by remember { mutableStateOf<String?>(null) }

    val filteredPosts = remember(selectedCategory, posts, followingIds) {
        when (selectedCategory) {
            "For You" -> posts
            "Following" -> posts.filter { followingIds.contains(it.authorId) }
            else -> posts.filter { it.gameTag.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = PrimeDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenUploadSheet(UploadType.POST) },
                containerColor = PrimeBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimeBlue,
                                selectedLabelColor = Color.White,
                                containerColor = PrimeCardBackground,
                                labelColor = PrimeTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) PrimeBlue else PrimeBorder
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Latest Creator Videos Strip (only shown when real videos exist)
            if (selectedCategory == "For You" && videos.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                        Text(
                            text = if (isHindi) "नए वीडियो" else "Latest Channel Videos",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(videos.take(10), key = { it.id }) { video ->
                                val creator = usersMap[video.ownerId]
                                val creatorName = creator?.username?.ifBlank { null } ?: video.authorUsername.ifBlank { "Creator" }
                                Card(
                                    modifier = Modifier
                                        .width(210.dp)
                                        .clickable {
                                            playingVideo = video
                                            coroutineScope.launch {
                                                creatorRepository.incrementVideoViews(video.id)
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(114.dp)
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
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = video.title,
                                                color = PrimeTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "@$creatorName • ${video.viewsCount} views",
                                                color = PrimeTextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (val currentPostsState = postsState) {
                is UiDataState.Loading -> {
                    item {
                        PrimeLoadingBox(
                            message = if (isHindi) "फीड लोड हो रहा है..." else "Loading gaming feed..."
                        )
                    }
                }
                is UiDataState.Error -> {
                    item {
                        PrimeErrorRetryCard(
                            message = currentPostsState.message,
                            onRetry = { retryTrigger++ }
                        )
                    }
                }
                is UiDataState.Success -> if (filteredPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎮", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "अभी तक कोई पोस्ट उपलब्ध नहीं है।" else "No posts available yet.",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = when (selectedCategory) {
                                "Following" -> "Follow creators to see their posts here, or publish your own!"
                                else -> "Be the first to share a post with the PRIME ESPORT community."
                            },
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        Button(
                            onClick = { onOpenUploadSheet(UploadType.POST) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    val isLiked by socialRepository.observeIsPostLiked(post.id).collectAsState(initial = false)
                    PostCard(
                        post = post,
                        authorProfile = usersMap[post.authorId],
                        currentUserId = currentUserId,
                        isLiked = isLiked,
                        onLikeClick = {
                            coroutineScope.launch {
                                socialRepository.toggleLikePost(post.id)
                            }
                        },
                        onCommentsClick = { selectedPostForComments = post },
                        onShareClick = {
                            val handle = usersMap[post.authorId]?.username ?: post.authorUsername
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out this post by @$handle on PRIME ESPORT: ${post.content}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                        },
                        onReportClick = { selectedPostForReport = post },
                        onDeleteClick = {
                            deletePostError = null
                            postPendingDelete = post
                        },
                        onAuthorClick = {
                            if (post.authorId.isNotBlank()) onOpenProfile(post.authorId)
                        }
                    )
                }
            }
            }
        }
    }

    selectedPostForComments?.let { post ->
        PostCommentsSheet(
            post = post,
            currentUserId = currentUserId,
            usersMap = usersMap,
            socialRepository = socialRepository,
            blockedUserIds = blockedUserIds,
            onOpenProfile = onOpenProfile,
            onDismiss = { selectedPostForComments = null }
        )
    }

    selectedPostForReport?.let { post ->
        ReportContentSheet(
            title = "Report Post",
            targetType = "post",
            targetId = post.id,
            safetyRepository = safetyRepository,
            onDismiss = { selectedPostForReport = null }
        )
    }

    postPendingDelete?.let { targetPost ->
        DeleteConfirmationDialog(
            title = "Delete Post",
            message = "Are you sure you want to permanently delete this post?",
            isDeleting = isDeletingPost,
            errorMessage = deletePostError,
            onConfirm = {
                isDeletingPost = true
                deletePostError = null
                coroutineScope.launch {
                    val res = socialRepository.deletePost(targetPost, mediaUploadService)
                    isDeletingPost = false
                    if (res.isSuccess) {
                        postPendingDelete = null
                    } else {
                        deletePostError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete post."
                    }
                }
            },
            onDismiss = { postPendingDelete = null }
        )
    }

    playingVideo?.let { video ->
        VideoPlayerModal(
            video = video,
            creatorProfile = usersMap[video.ownerId],
            isOwner = video.ownerId == currentUserId,
            onDismiss = { playingVideo = null },
            onOpenProfile = onOpenProfile,
            onDelete = {
                deleteVideoError = null
                videoPendingDelete = video
            }
        )
    }

    videoPendingDelete?.let { targetVideo ->
        DeleteConfirmationDialog(
            title = "Delete Video",
            message = "Are you sure you want to permanently delete '${targetVideo.title}'?",
            isDeleting = isDeletingVideo,
            errorMessage = deleteVideoError,
            onConfirm = {
                isDeletingVideo = true
                deleteVideoError = null
                coroutineScope.launch {
                    val res = creatorRepository.deleteVideo(targetVideo, mediaUploadService)
                    isDeletingVideo = false
                    if (res.isSuccess) {
                        videoPendingDelete = null
                        if (playingVideo?.id == targetVideo.id) {
                            playingVideo = null
                        }
                    } else {
                        deleteVideoError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete video."
                    }
                }
            },
            onDismiss = { videoPendingDelete = null }
        )
    }
}
