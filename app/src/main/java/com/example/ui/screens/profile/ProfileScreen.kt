package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.ChannelVideo
import com.example.data.model.Post
import com.example.data.model.Reel
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.UsernameCheckResult
import com.example.data.storage.MediaUploadService
import com.example.ui.components.AvatarCropDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.GamerAvatar
import com.example.ui.components.PostCard
import com.example.ui.components.PostCommentsSheet
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.PrimeVideoPlayer
import com.example.ui.components.ReportContentSheet
import com.example.ui.components.VerificationBadges
import com.example.ui.components.VideoPlayerModal
import com.example.ui.components.formatRelativeTime
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private enum class FollowListMode {
    FOLLOWERS,
    FOLLOWING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    creatorRepository: CreatorRepository,
    safetyRepository: SafetyRepository,
    userId: String,
    currentUserId: String,
    isHindi: Boolean = false,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenChat: (String, String, String) -> Unit = { _, _, _ -> onOpenMessages() },
    onOpenUploadSheet: (UploadType) -> Unit = {},
    onNavigateToCreatorStudio: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaUploadService = remember(context) { MediaUploadService(context) }

    val isOwnProfile = userId == currentUserId
    var retryTrigger by remember(userId) { mutableIntStateOf(0) }
    val userProfileState by remember(userId, retryTrigger) {
        authRepository.observeUserProfileState(userId)
    }.collectAsState(initial = UiDataState.Loading)
    val userProfile = (userProfileState as? UiDataState.Success)?.data
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())
    val userPostsState by remember(userId, retryTrigger) {
        socialRepository.observeUserPostsState(userId)
    }.collectAsState(initial = UiDataState.Loading)
    val userVideosState by remember(userId, retryTrigger) {
        creatorRepository.observeUserVideosState(userId)
    }.collectAsState(initial = UiDataState.Loading)
    val userReelsState by remember(userId, retryTrigger) {
        socialRepository.observeUserReelsState(userId)
    }.collectAsState(initial = UiDataState.Loading)

    val userPosts = (userPostsState as? UiDataState.Success)?.data ?: emptyList()
    val userVideos = (userVideosState as? UiDataState.Success)?.data ?: emptyList()
    val userReels = (userReelsState as? UiDataState.Success)?.data ?: emptyList()

    val followers by remember(userId) { socialRepository.observeFollowers(userId) }.collectAsState(initial = emptyList())
    val following by remember(userId) { socialRepository.observeFollowing(userId) }.collectAsState(initial = emptyList())
    val isFollowing by remember(userId) { socialRepository.observeIsFollowing(userId) }.collectAsState(initial = false)
    val isBlocked by remember(userId) { safetyRepository.observeIsUserBlocked(userId) }.collectAsState(initial = false)
    val blockedUserIds by safetyRepository.observeBlockedUserIds().collectAsState(initial = emptySet())

    val friendships by socialRepository.observeFriendships().collectAsState(initial = emptyList())
    val sentRequests by socialRepository.observeSentFriendRequests().collectAsState(initial = emptyList())
    val receivedRequests by socialRepository.observeReceivedFriendRequests().collectAsState(initial = emptyList())

    val isFriend = remember(friendships, userId) {
        friendships.any { it.users.contains(userId) }
    }
    val sentPendingRequest = remember(sentRequests, userId) {
        sentRequests.firstOrNull { it.receiverId == userId && it.status == "pending" }
    }
    val receivedPendingRequest = remember(receivedRequests, userId) {
        receivedRequests.firstOrNull { it.senderId == userId && it.status == "pending" }
    }

    var selectedTab by remember(userId) { mutableIntStateOf(0) }
    val tabTitles = listOf("Home", "Posts", "Videos", "Reels", "About")

    var showEditProfileDialog by remember(userId) { mutableStateOf(false) }
    var gamingIdentityEditorSection by remember(userId) { mutableStateOf<GamingIdentitySection?>(null) }
    var showOwnerVerificationSheet by remember(userId) { mutableStateOf(false) }
    var followListMode by remember(userId) { mutableStateOf<FollowListMode?>(null) }
    var showUnfriendConfirm by remember(userId) { mutableStateOf(false) }
    var showBlockDialog by remember(userId) { mutableStateOf(false) }
    var isBlockBusy by remember(userId) { mutableStateOf(false) }
    var isFriendActionBusy by remember(userId) { mutableStateOf(false) }
    var isFollowBusy by remember(userId) { mutableStateOf(false) }

    // Direct avatar tap -> PickVisualMedia -> Crop -> Upload
    var avatarRawCropUri by remember(userId) { mutableStateOf<Uri?>(null) }
    var isQuickAvatarUploading by remember(userId) { mutableStateOf(false) }
    val quickAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                avatarRawCropUri = uri
            }
        }
    )

    // Modals for posts, videos, reels, report, delete
    var selectedPostForComments by remember(userId) { mutableStateOf<Post?>(null) }
    var selectedPostForReport by remember(userId) { mutableStateOf<Post?>(null) }
    var showReportUserSheet by remember(userId) { mutableStateOf(false) }
    var postPendingDelete by remember(userId) { mutableStateOf<Post?>(null) }
    var isDeletingPost by remember(userId) { mutableStateOf(false) }
    var deletePostError by remember(userId) { mutableStateOf<String?>(null) }

    var playingVideo by remember(userId) { mutableStateOf<ChannelVideo?>(null) }
    var videoPendingDelete by remember(userId) { mutableStateOf<ChannelVideo?>(null) }
    var isDeletingVideo by remember(userId) { mutableStateOf(false) }
    var deleteVideoError by remember(userId) { mutableStateOf<String?>(null) }

    var playingReel by remember(userId) { mutableStateOf<Reel?>(null) }
    var reelPendingDelete by remember(userId) { mutableStateOf<Reel?>(null) }
    var isDeletingReel by remember(userId) { mutableStateOf(false) }
    var deleteReelError by remember(userId) { mutableStateOf<String?>(null) }

    val displayName = userProfile?.displayName?.ifBlank { userProfile.username.ifBlank { "Player" } } ?: "Player"
    val username = userProfile?.username ?: ""

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            if (showBackButton || !isOwnProfile) {
                Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                        }
                        Text(
                            text = when (userProfileState) {
                                is UiDataState.Loading -> "Loading profile..."
                                is UiDataState.Error -> "Player Profile"
                                is UiDataState.Success -> {
                                    if (userProfile == null) "Profile Unavailable"
                                    else if (username.isNotBlank()) "@$username"
                                    else displayName
                                }
                            },
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VerificationBadges(
                            isOwner = userProfile?.isOwner == true,
                            isVerified = userProfile?.isVerified == true
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val pState = userProfileState) {
            is UiDataState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    PrimeLoadingBox("Loading player profile...")
                }
            }
            is UiDataState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    PrimeErrorRetryCard(
                        message = pState.message,
                        onRetry = { retryTrigger++ }
                    )
                }
            }
            is UiDataState.Success -> {
                if (pState.data == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("👤", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Player profile not found.",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This player may not have completed setup yet or the account was removed.",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )
                        if (showBackButton) {
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Go Back", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile Channel Header & Optional Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimeNavySurface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimeNavySurface, PrimeBlue.copy(alpha = 0.35f), PrimePurple.copy(alpha = 0.35f))
                                )
                            )
                    ) {
                        val bannerUrl = userProfile?.bannerUrl.orEmpty()
                        if (bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = "Profile Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar (clickable on own profile to change photo with crop)
                        Box(contentAlignment = Alignment.BottomEnd) {
                            GamerAvatar(
                                avatarUrl = userProfile?.avatarUrl ?: "",
                                displayName = displayName,
                                size = 84,
                                isOnline = userProfile?.isOnline ?: false,
                                onClick = if (isOwnProfile && !isQuickAvatarUploading) {
                                    {
                                        quickAvatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                } else null
                            )
                            if (isOwnProfile) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PrimeBlue)
                                        .border(2.dp, PrimeNavySurface, CircleShape)
                                        .clickable(enabled = !isQuickAvatarUploading) {
                                            quickAvatarPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isQuickAvatarUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Profile Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayName,
                                color = PrimeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Text(
                            text = userProfile?.bio?.ifBlank { "No bio added yet." } ?: "No bio added yet.",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                        )

                        pState.data?.let { profile ->
                            ProfileHeaderFavoriteTitlesStrip(
                                userProfile = profile,
                                onOpenSocialLink = { link ->
                                    val raw = link.handleOrUrl.trim()
                                    val safeUrl = if (raw.startsWith("https://", true) || raw.startsWith("http://", true)) {
                                        raw
                                    } else {
                                        val handle = raw.removePrefix("@").trim()
                                        when (link.platform.trim().lowercase()) {
                                            "youtube" -> "https://www.youtube.com/@$handle"
                                            "twitch" -> "https://www.twitch.tv/$handle"
                                            "x / twitter", "twitter", "x" -> "https://x.com/$handle"
                                            "instagram" -> "https://www.instagram.com/$handle"
                                            "kick" -> "https://kick.com/$handle"
                                            "github" -> "https://github.com/$handle"
                                            else -> null
                                        }
                                    }
                                    if (safeUrl != null) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                        }.onFailure {
                                            Toast.makeText(context, "${link.platform}: ${link.handleOrUrl}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "${link.platform}: ${link.handleOrUrl}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        // Real Channel & Social Stats (2 tidy rows for all screen widths)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimeCardBackground)
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStatItem(
                                    title = "Followers",
                                    count = maxOf(followers.size, userProfile?.followersCount ?: 0),
                                    onClick = { followListMode = FollowListMode.FOLLOWERS }
                                )
                                ProfileStatItem(
                                    title = "Following",
                                    count = maxOf(following.size, userProfile?.followingCount ?: 0),
                                    onClick = { followListMode = FollowListMode.FOLLOWING }
                                )
                                ProfileStatItem(
                                    title = "Friends",
                                    count = userProfile?.friendsCount ?: 0
                                )
                            }
                            HorizontalDivider(color = PrimeBorder.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStatItem(title = "Videos", count = userVideos.size)
                                ProfileStatItem(title = "Posts", count = userPosts.size)
                                ProfileStatItem(title = "Reels", count = userReels.size)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons
                        if (isOwnProfile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(1f).testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = { onOpenUploadSheet(UploadType.MENU) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimeRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.weight(0.85f).testTag("profile_upload_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = onNavigateToCreatorStudio,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("profile_creator_studio_button")
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Studio", tint = PrimeBlue, modifier = Modifier.size(18.dp))
                                }

                                OutlinedButton(
                                    onClick = onNavigateToSettings,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.testTag("profile_settings_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimeTextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { gamingIdentityEditorSection = GamingIdentitySection.HANDLES },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_gaming_identity_button")
                            ) {
                                Text(
                                    text = "Gaming Handles, Ranks & Social Links",
                                    color = PrimeBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Official Owner Verification Management CTA (strictly restricted to Official Owner)
                            if (userProfile?.isOwner == true && authRepository.isCurrentUserOfficialOwner()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { showOwnerVerificationSheet = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimePurple),
                                    modifier = Modifier.fillMaxWidth().testTag("owner_verification_panel_button")
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = PrimePurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Owner Verification Panel", color = PrimePurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        } else {
                            // Visiting another creator's profile
                            if (isBlocked) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Block, contentDescription = null, tint = PrimeRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "You blocked this user.",
                                                color = PrimeTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Button(
                                            onClick = { showBlockDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimeNavySurface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("profile_unblock_button")
                                        ) {
                                            Text("Unblock", color = PrimeRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Follow / Unfollow
                                    Button(
                                        onClick = {
                                            isFollowBusy = true
                                            coroutineScope.launch {
                                                val res = socialRepository.toggleFollow(userId)
                                                isFollowBusy = false
                                                if (res.isFailure) {
                                                    Toast.makeText(
                                                        context,
                                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to update follow status.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        enabled = !isFollowBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) PrimeCardBackground else PrimePurple
                                        ),
                                        border = if (isFollowing) androidx.compose.foundation.BorderStroke(1.dp, PrimePurple) else null,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("profile_follow_button")
                                    ) {
                                        Text(
                                            text = if (isFollowing) "Following" else "Follow",
                                            color = if (isFollowing) PrimePurple else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    // Stateful Friend Action
                                    when {
                                        isFriend -> {
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val res = socialRepository.getOrCreateConversationWithFriend(userId)
                                                        if (res.isSuccess) {
                                                            val convId = res.getOrThrow()
                                                            val title = if (username.isNotBlank()) "$displayName (@$username)" else displayName
                                                            onOpenChat(convId, userId, title)
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                res.exceptionOrNull()?.localizedMessage ?: "Unable to open chat.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f).testTag("profile_message_button")
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Message", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { showUnfriendConfirm = true },
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed.copy(alpha = 0.6f))
                                            ) {
                                                Icon(Icons.Default.PersonRemove, contentDescription = "Unfriend", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        sentPendingRequest != null -> {
                                            OutlinedButton(
                                                onClick = {
                                                    isFriendActionBusy = true
                                                    coroutineScope.launch {
                                                        socialRepository.cancelFriendRequest(sentPendingRequest.id)
                                                        isFriendActionBusy = false
                                                    }
                                                },
                                                enabled = !isFriendActionBusy,
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel Request", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        receivedPendingRequest != null -> {
                                            Button(
                                                onClick = {
                                                    isFriendActionBusy = true
                                                    coroutineScope.launch {
                                                        socialRepository.acceptFriendRequest(receivedPendingRequest.id, userId)
                                                        isFriendActionBusy = false
                                                    }
                                                },
                                                enabled = !isFriendActionBusy,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        else -> {
                                            Button(
                                                onClick = {
                                                    isFriendActionBusy = true
                                                    coroutineScope.launch {
                                                        val res = socialRepository.sendFriendRequest(userId)
                                                        isFriendActionBusy = false
                                                        if (res.isFailure) {
                                                            Toast.makeText(
                                                                context,
                                                                res.exceptionOrNull()?.localizedMessage ?: "Failed to send request.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                enabled = !isFriendActionBusy,
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f).testTag("profile_add_friend_button")
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Add Friend", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { showBlockDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("profile_block_button")
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = "Block User", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                    }

                                    OutlinedButton(
                                        onClick = { showReportUserSheet = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier.testTag("profile_report_button")
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = "Report User", tint = PrimeTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5 Channel Tabs: Home, Posts, Videos, Reels, About
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrimeCardBackground,
                    contentColor = PrimeBlue,
                    edgePadding = 12.dp,
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

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Home Tab: Gaming Identity Preview + Creator Channel Overview
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pState.data?.let { profile ->
                                ProfileGamingIdentityShowcase(
                                    userProfile = profile,
                                    isOwnProfile = isOwnProfile,
                                    compactPreview = true,
                                    onEditSection = { section -> gamingIdentityEditorSection = section }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Featured Videos", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (userVideos.isEmpty()) {
                                EmptyProfileTabBox("No videos uploaded yet.")
                            } else {
                                userVideos.take(2).forEach { video ->
                                    ProfileVideoCard(
                                        video = video,
                                        isOwner = isOwnProfile,
                                        onPlay = {
                                            playingVideo = video
                                            coroutineScope.launch { creatorRepository.incrementVideoViews(video.id) }
                                        },
                                        onDelete = {
                                            deleteVideoError = null
                                            videoPendingDelete = video
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Latest Posts", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (userPosts.isEmpty()) {
                                EmptyProfileTabBox("No posts available yet.")
                            }
                        }
                    }
                    if (userPosts.isNotEmpty()) {
                        items(userPosts.take(2), key = { "home_${it.id}" }) { post ->
                            val isLiked by socialRepository.observeIsPostLiked(post.id).collectAsState(initial = false)
                            PostCard(
                                post = post,
                                authorProfile = userProfile,
                                currentUserId = currentUserId,
                                isLiked = isLiked,
                                onLikeClick = { coroutineScope.launch { socialRepository.toggleLikePost(post.id) } },
                                onCommentsClick = { selectedPostForComments = post },
                                onShareClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Check out @$username's post on PRIME ESPORT: ${post.content}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                                },
                                onReportClick = { selectedPostForReport = post },
                                onDeleteClick = {
                                    deletePostError = null
                                    postPendingDelete = post
                                },
                                onAuthorClick = { if (post.authorId.isNotBlank() && post.authorId != userId) onOpenProfile(post.authorId) }
                            )
                        }
                    }
                }
                1 -> {
                    // Posts Tab
                    when (val pState = userPostsState) {
                        is UiDataState.Loading -> {
                            item { PrimeLoadingBox("Loading posts...") }
                        }
                        is UiDataState.Error -> {
                            item { PrimeErrorRetryCard(pState.message, onRetry = { retryTrigger++ }) }
                        }
                        is UiDataState.Success -> {
                            if (pState.data.isEmpty()) {
                                item { EmptyProfileTabBox("No posts available yet.", modifier = Modifier.padding(32.dp)) }
                            } else {
                                items(pState.data, key = { it.id }) { post ->
                                    val isLiked by socialRepository.observeIsPostLiked(post.id).collectAsState(initial = false)
                                    PostCard(
                                        post = post,
                                        authorProfile = userProfile,
                                        currentUserId = currentUserId,
                                        isLiked = isLiked,
                                        onLikeClick = { coroutineScope.launch { socialRepository.toggleLikePost(post.id) } },
                                        onCommentsClick = { selectedPostForComments = post },
                                        onShareClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Check out @$username's post on PRIME ESPORT: ${post.content}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                                        },
                                        onReportClick = { selectedPostForReport = post },
                                        onDeleteClick = {
                                            deletePostError = null
                                            postPendingDelete = post
                                        },
                                        onAuthorClick = { if (post.authorId.isNotBlank() && post.authorId != userId) onOpenProfile(post.authorId) }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Videos Tab
                    when (val vState = userVideosState) {
                        is UiDataState.Loading -> {
                            item { PrimeLoadingBox("Loading videos...") }
                        }
                        is UiDataState.Error -> {
                            item { PrimeErrorRetryCard(vState.message, onRetry = { retryTrigger++ }) }
                        }
                        is UiDataState.Success -> {
                            if (vState.data.isEmpty()) {
                                item { EmptyProfileTabBox("No videos uploaded yet.", modifier = Modifier.padding(32.dp)) }
                            } else {
                                items(vState.data, key = { it.id }) { video ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        ProfileVideoCard(
                                            video = video,
                                            isOwner = isOwnProfile,
                                            onPlay = {
                                                playingVideo = video
                                                coroutineScope.launch { creatorRepository.incrementVideoViews(video.id) }
                                            },
                                            onDelete = {
                                                deleteVideoError = null
                                                videoPendingDelete = video
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Reels Tab
                    when (val rState = userReelsState) {
                        is UiDataState.Loading -> {
                            item { PrimeLoadingBox("Loading Reels...") }
                        }
                        is UiDataState.Error -> {
                            item { PrimeErrorRetryCard(rState.message, onRetry = { retryTrigger++ }) }
                        }
                        is UiDataState.Success -> {
                            if (rState.data.isEmpty()) {
                                item { EmptyProfileTabBox("No Reels available yet.", modifier = Modifier.padding(32.dp)) }
                            } else {
                                items(rState.data, key = { it.id }) { reel ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable { playingReel = reel },
                                colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(PrimeNavySurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Reel", tint = PrimeRed, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reel.caption,
                                            color = PrimeTextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (reel.tags.isNotBlank()) {
                                            Text(
                                                text = reel.tags,
                                                color = PrimeBlue,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${reel.likesCount} likes • ${reel.commentsCount} comments • ${formatRelativeTime(reel.createdAt)}",
                                            color = PrimeTextMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (isOwnProfile) {
                                        IconButton(onClick = {
                                            deleteReelError = null
                                            reelPendingDelete = reel
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Reel", tint = PrimeRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                        }
                    }
                }
                4 -> {
                    // About Tab
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pState.data?.let { profile ->
                                ProfileGamingIdentityShowcase(
                                    userProfile = profile,
                                    isOwnProfile = isOwnProfile,
                                    compactPreview = false,
                                    onEditSection = { section -> gamingIdentityEditorSection = section }
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("About Creator Channel", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = userProfile?.bio?.ifBlank { "No bio added yet." } ?: "No bio added yet.",
                                        color = PrimeTextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                    if (!userProfile?.favoriteGames.isNullOrBlank()) {
                                        Text(
                                            text = "Gaming Interests: ${userProfile?.favoriteGames}",
                                            color = PrimePurple,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val joinedDate = userProfile?.createdAt?.toDate()?.let {
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                                    }
                                    if (joinedDate != null) {
                                        Text(
                                            text = "Joined PRIME ESPORT: $joinedDate",
                                            color = PrimeTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            val earnedBadges = buildList {
                                if (username.isNotBlank()) {
                                    add(Triple("🎮", "Registered Player", "Claimed unique @$username handle"))
                                }
                                if (userVideos.isNotEmpty()) {
                                    add(Triple("🎥", "Video Creator", "Uploaded ${userVideos.size} video(s)"))
                                }
                                if (userPosts.isNotEmpty()) {
                                    add(Triple("📝", "Community Contributor", "Published ${userPosts.size} post(s)"))
                                }
                                if (userReels.isNotEmpty()) {
                                    add(Triple("🎬", "Clip Creator", "Uploaded ${userReels.size} gaming Reel(s)"))
                                }
                                if ((userProfile?.friendsCount ?: 0) > 0) {
                                    add(Triple("🤝", "Squad Teammate", "Connected with ${userProfile?.friendsCount} friend(s)"))
                                }
                            }
                            if (earnedBadges.isNotEmpty()) {
                                Text("Channel Milestones", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                earnedBadges.forEach { (icon, title, desc) ->
                                    AchievementBadge(icon = icon, title = title, desc = desc)
                                }
                            }
                        }
                    }
                }
            }
        }
                }
            }
        }
    }

    // Quick Avatar Crop & Immediate Upload
    avatarRawCropUri?.let { rawUri ->
        AvatarCropDialog(
            sourceUri = rawUri,
            isCircleAvatar = true,
            onDismiss = { avatarRawCropUri = null },
            onCropped = { croppedUri ->
                avatarRawCropUri = null
                isQuickAvatarUploading = true
                coroutineScope.launch {
                    val uploadRes = mediaUploadService.uploadMedia(
                        uri = croppedUri,
                        folder = "avatars",
                        userId = currentUserId
                    )
                    if (uploadRes.isSuccess) {
                        val uploaded = uploadRes.getOrThrow()
                        val updateRes = authRepository.updateUserProfile(
                            displayName = userProfile?.displayName?.ifBlank { username } ?: username,
                            bio = userProfile?.bio ?: "",
                            avatarUrl = uploaded.downloadUrl,
                            favoriteGames = userProfile?.favoriteGames ?: "",
                            bannerUrl = userProfile?.bannerUrl ?: ""
                        )
                        if (updateRes.isSuccess) {
                            Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                updateRes.exceptionOrNull()?.localizedMessage ?: "Failed to save profile picture.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            uploadRes.exceptionOrNull()?.localizedMessage ?: "Failed to upload photo.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isQuickAvatarUploading = false
                }
            }
        )
    }

    // Full Edit Profile Sheet (Avatar Crop + Banner Crop + Username + Display Name + Bio + Games)
    if (showEditProfileDialog) {
        EditProfileBottomSheet(
            userProfile = userProfile,
            currentUserId = currentUserId,
            authRepository = authRepository,
            mediaUploadService = mediaUploadService,
            onDismiss = { showEditProfileDialog = false },
            onSaved = {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                showEditProfileDialog = false
            }
        )
    }

    gamingIdentityEditorSection?.let { section ->
        GamingIdentityEditorBottomSheet(
            userProfile = userProfile,
            initialSection = section,
            authRepository = authRepository,
            onDismiss = { gamingIdentityEditorSection = null },
            onSaved = {
                Toast.makeText(context, "Gaming profile saved!", Toast.LENGTH_SHORT).show()
                gamingIdentityEditorSection = null
            }
        )
    }

    // Owner Verification Panel (only accessible to Official Owner)
    if (showOwnerVerificationSheet && userProfile?.isOwner == true && authRepository.isCurrentUserOfficialOwner()) {
        OwnerVerificationBottomSheet(
            users = usersMap.values.sortedBy { it.username.lowercase() },
            authRepository = authRepository,
            onOpenProfile = { uid ->
                showOwnerVerificationSheet = false
                onOpenProfile(uid)
            },
            onDismiss = { showOwnerVerificationSheet = false }
        )
    }

    // Followers / Following Bottom Sheet
    followListMode?.let { mode ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val ids = if (mode == FollowListMode.FOLLOWERS) followers.map { it.followerId } else following.map { it.followingId }
        val profiles = ids.distinct().map { uid ->
            usersMap[uid] ?: UserProfile(
                userId = uid,
                username = "player_${uid.take(6)}",
                displayName = "Player"
            )
        }
        ModalBottomSheet(
            onDismissRequest = { followListMode = null },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (mode == FollowListMode.FOLLOWERS) "Followers (${profiles.size})" else "Following (${profiles.size})",
                    color = PrimeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (profiles.isEmpty()) {
                    Text(
                        text = if (mode == FollowListMode.FOLLOWERS) "No followers yet." else "Not following anyone yet.",
                        color = PrimeTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(340.dp)) {
                        items(profiles, key = { it.userId }) { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        followListMode = null
                                        onOpenProfile(target.userId)
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GamerAvatar(
                                    avatarUrl = target.avatarUrl,
                                    displayName = target.displayName.ifBlank { target.username },
                                    size = 42
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = target.displayName.ifBlank { target.username },
                                            color = PrimeTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        VerificationBadges(
                                            isOwner = target.isOwner,
                                            isVerified = target.isVerified
                                        )
                                    }
                                    Text("@${target.username}", color = PrimeBlue, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Unfriend confirmation
    if (showUnfriendConfirm) {
        DeleteConfirmationDialog(
            title = "Remove Friend",
            message = "Are you sure you want to remove @$username from your friends list?",
            isDeleting = isFriendActionBusy,
            onConfirm = {
                isFriendActionBusy = true
                coroutineScope.launch {
                    socialRepository.removeFriend(userId)
                    isFriendActionBusy = false
                    showUnfriendConfirm = false
                }
            },
            onDismiss = { showUnfriendConfirm = false }
        )
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

    if (showReportUserSheet) {
        ReportContentSheet(
            title = "Report Creator",
            targetType = "user",
            targetId = userId,
            safetyRepository = safetyRepository,
            onDismiss = { showReportUserSheet = false }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBlockBusy) showBlockDialog = false },
            containerColor = PrimeNavySurface,
            title = {
                Text(
                    text = if (isBlocked) "Unblock @$username?" else "Block @$username?",
                    color = PrimeTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isBlocked) {
                        "Unblocking allows their posts and interactions to appear again."
                    } else {
                        "Blocking hides their posts, reels, and messages from your feeds."
                    },
                    color = PrimeTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isBlockBusy = true
                        coroutineScope.launch {
                            val res = if (isBlocked) {
                                safetyRepository.unblockUser(userId)
                            } else {
                                safetyRepository.blockUser(userId)
                            }
                            isBlockBusy = false
                            if (res.isSuccess) {
                                showBlockDialog = false
                                Toast.makeText(
                                    context,
                                    if (isBlocked) "User unblocked" else "User blocked",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    res.exceptionOrNull()?.localizedMessage ?: "Failed to update block status.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isBlockBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeRed)
                ) {
                    Text(if (isBlocked) "Unblock" else "Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }, enabled = !isBlockBusy) {
                    Text("Cancel", color = PrimeTextSecondary)
                }
            }
        )
    }

    postPendingDelete?.let { post ->
        DeleteConfirmationDialog(
            title = "Delete Post",
            message = "Permanently delete this post from your profile?",
            isDeleting = isDeletingPost,
            errorMessage = deletePostError,
            onConfirm = {
                isDeletingPost = true
                deletePostError = null
                coroutineScope.launch {
                    val res = socialRepository.deletePost(post, mediaUploadService)
                    isDeletingPost = false
                    if (res.isSuccess) postPendingDelete = null
                    else deletePostError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete post."
                }
            },
            onDismiss = { postPendingDelete = null }
        )
    }

    playingVideo?.let { video ->
        VideoPlayerModal(
            video = video,
            creatorProfile = userProfile,
            isOwner = isOwnProfile,
            onDismiss = { playingVideo = null },
            onOpenProfile = onOpenProfile,
            onDelete = {
                deleteVideoError = null
                videoPendingDelete = video
            }
        )
    }

    videoPendingDelete?.let { video ->
        DeleteConfirmationDialog(
            title = "Delete Video",
            message = "Permanently delete '${video.title}'?",
            isDeleting = isDeletingVideo,
            errorMessage = deleteVideoError,
            onConfirm = {
                isDeletingVideo = true
                deleteVideoError = null
                coroutineScope.launch {
                    val res = creatorRepository.deleteVideo(video, mediaUploadService)
                    isDeletingVideo = false
                    if (res.isSuccess) {
                        videoPendingDelete = null
                        if (playingVideo?.id == video.id) playingVideo = null
                    } else {
                        deleteVideoError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete video."
                    }
                }
            },
            onDismiss = { videoPendingDelete = null }
        )
    }

    playingReel?.let { reel ->
        Dialog(
            onDismissRequest = { playingReel = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PrimeVideoPlayer(
                        videoUrl = reel.videoUrl,
                        autoPlay = true,
                        showController = true,
                        loop = true,
                        thumbnailUrl = reel.thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { playingReel = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Reel", tint = Color.White)
                    }
                }
            }
        }
    }

    reelPendingDelete?.let { reel ->
        DeleteConfirmationDialog(
            title = "Delete Reel",
            message = "Permanently delete this Reel?",
            isDeleting = isDeletingReel,
            errorMessage = deleteReelError,
            onConfirm = {
                isDeletingReel = true
                deleteReelError = null
                coroutineScope.launch {
                    val res = socialRepository.deleteReel(reel, mediaUploadService)
                    isDeletingReel = false
                    if (res.isSuccess) reelPendingDelete = null
                    else deleteReelError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete Reel."
                }
            },
            onDismiss = { reelPendingDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    userProfile: UserProfile?,
    currentUserId: String,
    authRepository: AuthRepository,
    mediaUploadService: MediaUploadService,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialUsername = userProfile?.username ?: ""
    var usernameInput by remember { mutableStateOf(initialUsername) }
    var displayName by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var favoriteGames by remember { mutableStateOf(userProfile?.favoriteGames ?: "") }

    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var pendingBannerUri by remember { mutableStateOf<Uri?>(null) }
    var currentAvatarUrl by remember { mutableStateOf(userProfile?.avatarUrl ?: "") }
    var currentBannerUrl by remember { mutableStateOf(userProfile?.bannerUrl ?: "") }

    var rawAvatarCropUri by remember { mutableStateOf<Uri?>(null) }
    var rawBannerCropUri by remember { mutableStateOf<Uri?>(null) }

    var usernameStatus by remember { mutableStateOf<UsernameCheckResult>(UsernameCheckResult.Empty) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) rawAvatarCropUri = uri }
    )
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) rawBannerCropUri = uri }
    )

    LaunchedEffect(usernameInput) {
        val trimmed = usernameInput.trim()
        if (trimmed.equals(initialUsername, ignoreCase = true) || trimmed.isEmpty()) {
            usernameStatus = UsernameCheckResult.Empty
            isCheckingUsername = false
            return@LaunchedEffect
        }
        isCheckingUsername = true
        delay(350)
        usernameStatus = authRepository.checkUsernameAvailability(trimmed)
        isCheckingUsername = false
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edit Creator Profile",
                color = PrimeTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Avatar & Banner Pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PrimeNavySurface)
                        .border(1.5.dp, PrimeBlue, CircleShape)
                        .clickable(enabled = !isSaving) {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val previewModel: Any? = pendingAvatarUri ?: currentAvatarUrl.ifBlank { null }
                    if (previewModel != null) {
                        AsyncImage(
                            model = previewModel,
                            contentDescription = "Avatar preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Pick avatar", tint = PrimeBlue)
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_pick_avatar_button")
                    ) {
                        Text("Change Profile Photo", color = PrimeBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_pick_banner_button")
                    ) {
                        Text(
                            text = if (pendingBannerUri != null || currentBannerUrl.isNotBlank()) "Change Banner" else "Add Banner (Optional)",
                            color = PrimePurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { input ->
                    if (input.length <= 20) {
                        usernameInput = input.filter { it.isLetterOrDigit() || it == '_' }
                    }
                },
                label = { Text("Username") },
                singleLine = true,
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimeBlue)
                    } else {
                        when (usernameStatus) {
                            is UsernameCheckResult.Available -> Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = Color(0xFF10B981))
                            is UsernameCheckResult.Taken,
                            is UsernameCheckResult.Reserved,
                            is UsernameCheckResult.InvalidFormat -> Icon(Icons.Default.Error, contentDescription = "Invalid", tint = PrimeRed)
                            else -> {}
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimeTextPrimary,
                    unfocusedTextColor = PrimeTextPrimary,
                    focusedBorderColor = PrimeBlue,
                    unfocusedBorderColor = PrimeBorder
                )
            )

            AnimatedVisibility(visible = usernameStatus !is UsernameCheckResult.Empty) {
                val msg = when (val s = usernameStatus) {
                    is UsernameCheckResult.Available -> "@${s.normalized} is available"
                    is UsernameCheckResult.Taken -> s.message
                    is UsernameCheckResult.Reserved -> s.message
                    is UsernameCheckResult.InvalidFormat -> s.message
                    else -> ""
                }
                val color = if (usernameStatus is UsernameCheckResult.Available) Color(0xFF10B981) else PrimeRed
                if (msg.isNotBlank()) {
                    Text(msg, color = color, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimeTextPrimary,
                    unfocusedTextColor = PrimeTextPrimary,
                    focusedBorderColor = PrimeBlue,
                    unfocusedBorderColor = PrimeBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimeTextPrimary,
                    unfocusedTextColor = PrimeTextPrimary,
                    focusedBorderColor = PrimeBlue,
                    unfocusedBorderColor = PrimeBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = favoriteGames,
                onValueChange = { favoriteGames = it },
                label = { Text("Gaming Interests") },
                placeholder = { Text("e.g. BGMI, Minecraft, Valorant") },
                modifier = Modifier.fillMaxWidth().testTag("edit_games_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimeTextPrimary,
                    unfocusedTextColor = PrimeTextPrimary,
                    focusedBorderColor = PrimePurple,
                    unfocusedBorderColor = PrimeBorder
                )
            )

            if (saveError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(saveError ?: "", color = PrimeRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            val usernameInvalid = !usernameInput.trim().equals(initialUsername, ignoreCase = true) &&
                (isCheckingUsername || usernameStatus !is UsernameCheckResult.Available)

            Button(
                onClick = {
                    isSaving = true
                    saveError = null
                    coroutineScope.launch {
                        var finalAvatarUrl = currentAvatarUrl
                        var finalBannerUrl = currentBannerUrl

                        if (pendingAvatarUri != null) {
                            val avatarRes = mediaUploadService.uploadMedia(
                                uri = pendingAvatarUri!!,
                                folder = "avatars",
                                userId = currentUserId
                            )
                            if (avatarRes.isFailure) {
                                isSaving = false
                                saveError = avatarRes.exceptionOrNull()?.localizedMessage ?: "Failed to upload avatar."
                                return@launch
                            }
                            finalAvatarUrl = avatarRes.getOrThrow().downloadUrl
                        }

                        if (pendingBannerUri != null) {
                            val bannerRes = mediaUploadService.uploadMedia(
                                uri = pendingBannerUri!!,
                                folder = "banners",
                                userId = currentUserId
                            )
                            if (bannerRes.isFailure) {
                                isSaving = false
                                saveError = bannerRes.exceptionOrNull()?.localizedMessage ?: "Failed to upload banner."
                                return@launch
                            }
                            finalBannerUrl = bannerRes.getOrThrow().downloadUrl
                        }

                        val res = authRepository.updateUserProfile(
                            displayName = displayName,
                            bio = bio,
                            avatarUrl = finalAvatarUrl,
                            favoriteGames = favoriteGames,
                            bannerUrl = finalBannerUrl,
                            newUsername = usernameInput
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            onSaved()
                        } else {
                            saveError = res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile."
                        }
                    }
                },
                enabled = !isSaving && displayName.isNotBlank() && usernameInput.isNotBlank() && !usernameInvalid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    rawAvatarCropUri?.let { uri ->
        AvatarCropDialog(
            sourceUri = uri,
            isCircleAvatar = true,
            onDismiss = { rawAvatarCropUri = null },
            onCropped = { cropped ->
                pendingAvatarUri = cropped
                rawAvatarCropUri = null
            }
        )
    }

    rawBannerCropUri?.let { uri ->
        AvatarCropDialog(
            sourceUri = uri,
            isCircleAvatar = false,
            onDismiss = { rawBannerCropUri = null },
            onCropped = { cropped ->
                pendingBannerUri = cropped
                rawBannerCropUri = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerVerificationBottomSheet(
    users: List<UserProfile>,
    authRepository: AuthRepository,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var busyUserId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(users, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) users else users.filter {
            it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimePurple, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Official Owner Verification", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                text = "Only the Official Owner can grant or revoke creator verification badges.",
                color = PrimeTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search users by username...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimeTextPrimary,
                    unfocusedTextColor = PrimeTextPrimary,
                    focusedBorderColor = PrimePurple,
                    unfocusedBorderColor = PrimeBorder
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.height(340.dp)) {
                items(filtered, key = { it.userId }) { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GamerAvatar(
                            avatarUrl = target.avatarUrl,
                            displayName = target.displayName.ifBlank { target.username },
                            size = 40,
                            onClick = { onOpenProfile(target.userId) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenProfile(target.userId) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.displayName.ifBlank { target.username },
                                    color = PrimeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                VerificationBadges(isOwner = target.isOwner, isVerified = target.isVerified)
                            }
                            Text("@${target.username}", color = PrimeBlue, fontSize = 12.sp)
                        }
                        if (target.isOwner) {
                            Text("Owner", color = PrimePurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Switch(
                                checked = target.isVerified,
                                enabled = busyUserId != target.userId,
                                onCheckedChange = { checked ->
                                    busyUserId = target.userId
                                    coroutineScope.launch {
                                        val res = authRepository.setUserVerified(target.userId, checked)
                                        busyUserId = null
                                        if (res.isFailure) {
                                            Toast.makeText(
                                                context,
                                                res.exceptionOrNull()?.localizedMessage ?: "Failed to update verification.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimeBlue
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileVideoCard(
    video: ChannelVideo,
    isOwner: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(video.title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${video.viewsCount} views • ${video.category.ifBlank { "Video" }} • ${formatRelativeTime(video.createdAt)}",
                        color = PrimeTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (isOwner) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Video", tint = PrimeRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileTabBox(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = PrimeTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProfileStatItem(title: String, count: Int, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$count",
            color = PrimeTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = title,
            color = if (onClick != null) PrimeBlue else PrimeTextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun AchievementBadge(icon: String, title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeNavySurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, color = PrimeTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
