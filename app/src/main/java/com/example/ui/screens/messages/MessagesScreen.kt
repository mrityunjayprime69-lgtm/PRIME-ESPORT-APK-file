package com.example.ui.screens.messages

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UiDataState
import com.example.data.repository.AuthRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.GamerAvatar
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.VerificationBadges
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    safetyRepository: SafetyRepository? = null,
    currentUserId: String,
    initialConversationId: String? = null,
    initialOtherUserId: String? = null,
    initialChatTitle: String? = null,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())
    val conversationsState by remember(retryTrigger) {
        socialRepository.observeConversationsState()
    }.collectAsState(initial = UiDataState.Loading)
    val receivedState by remember(retryTrigger) {
        socialRepository.observeReceivedFriendRequestsState()
    }.collectAsState(initial = UiDataState.Loading)
    val sentState by remember(retryTrigger) {
        socialRepository.observeSentFriendRequestsState()
    }.collectAsState(initial = UiDataState.Loading)
    val friendshipsState by remember(retryTrigger) {
        socialRepository.observeFriendshipsState()
    }.collectAsState(initial = UiDataState.Loading)
    val blockedUserIds by remember(safetyRepository) {
        safetyRepository?.observeBlockedUserIds() ?: flowOf(emptySet())
    }.collectAsState(initial = emptySet())

    val rawReceivedRequests = (receivedState as? UiDataState.Success)?.data ?: emptyList()
    val visibleReceivedRequests = rawReceivedRequests.filter { !blockedUserIds.contains(it.senderId) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Messages", "Requests (${visibleReceivedRequests.size})", "Friends")

    var activeConversationId by remember(initialConversationId) { mutableStateOf(initialConversationId) }
    var activeChatOtherUserId by remember(initialOtherUserId) { mutableStateOf(initialOtherUserId ?: "") }
    var activeChatTitle by remember(initialChatTitle) { mutableStateOf(initialChatTitle ?: "Direct Chat") }
    var friendToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isRemovingFriend by remember { mutableStateOf(false) }

    if (activeConversationId != null) {
        ActiveChatScreen(
            conversationId = activeConversationId!!,
            otherUserId = activeChatOtherUserId,
            title = activeChatTitle,
            socialRepository = socialRepository,
            currentUserId = currentUserId,
            onOpenProfile = onOpenProfile,
            onBack = { activeConversationId = null }
        )
    } else {
        Scaffold(
            containerColor = PrimeDarkBackground,
            topBar = {
                Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                        }
                        Column {
                            Text("PRIME MESSAGES", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Privacy Protected • Friend-Gated Chat", color = PrimeBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimeCardBackground)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Direct chats require accepted friend request to prevent unsolicited messages.",
                            color = PrimeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PrimeNavySurface,
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
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (val convState = conversationsState) {
                                is UiDataState.Loading -> {
                                    item { PrimeLoadingBox("Loading conversations...") }
                                }
                                is UiDataState.Error -> {
                                    item { PrimeErrorRetryCard(convState.message, onRetry = { retryTrigger++ }) }
                                }
                                is UiDataState.Success -> {
                                    val visibleConversations = convState.data.filter { conv ->
                                        val otherUid = conv.participants.firstOrNull { it != currentUserId } ?: ""
                                        !blockedUserIds.contains(otherUid)
                                    }
                                    if (visibleConversations.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("💬", fontSize = 48.sp)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("No conversations yet", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text(
                                                    "Accept a friend request to start chatting!",
                                                    color = PrimeTextSecondary,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        items(visibleConversations, key = { it.id }) { conv ->
                                            val otherUserId = conv.participants.firstOrNull { it != currentUserId } ?: ""
                                            val profile = usersMap[otherUserId]
                                            val displayName = profile?.displayName?.ifBlank { profile.username } ?: "Friend"
                                            val handle = profile?.username ?: ""

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        activeConversationId = conv.id
                                                        activeChatOtherUserId = otherUserId
                                                        activeChatTitle = if (handle.isNotBlank()) "$displayName (@$handle)" else displayName
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    GamerAvatar(
                                                        avatarUrl = profile?.avatarUrl ?: "",
                                                        displayName = displayName,
                                                        size = 44,
                                                        isOnline = profile?.isOnline == true,
                                                        onClick = if (otherUserId.isNotBlank()) {
                                                            { onOpenProfile(otherUserId) }
                                                        } else null
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = displayName,
                                                                color = PrimeTextPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                            VerificationBadges(
                                                                isOwner = profile?.isOwner == true,
                                                                isVerified = profile?.isVerified == true
                                                            )
                                                        }
                                                        Text(
                                                            text = conv.lastMessage.ifBlank { "Tap to message" },
                                                            color = PrimeTextSecondary,
                                                            fontSize = 13.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Text(
                                                        text = formatRelativeTime(conv.updatedAt),
                                                        color = PrimeTextMuted,
                                                        fontSize = 11.sp
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
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            if (receivedState is UiDataState.Loading || sentState is UiDataState.Loading) {
                                item { PrimeLoadingBox("Loading friend requests...") }
                            } else if (receivedState is UiDataState.Error) {
                                item { PrimeErrorRetryCard((receivedState as UiDataState.Error).message, onRetry = { retryTrigger++ }) }
                            } else if (sentState is UiDataState.Error) {
                                item { PrimeErrorRetryCard((sentState as UiDataState.Error).message, onRetry = { retryTrigger++ }) }
                            } else {
                                val receivedRequests = visibleReceivedRequests
                                val sentRequests = ((sentState as? UiDataState.Success)?.data ?: emptyList())
                                    .filter { !blockedUserIds.contains(it.receiverId) }
                                if (receivedRequests.isEmpty() && sentRequests.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("🤝", fontSize = 48.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("No pending friend requests", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("Incoming and sent friend requests appear here.", color = PrimeTextSecondary, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    if (receivedRequests.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "Incoming Requests (${receivedRequests.size})",
                                                color = PrimeBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                    items(receivedRequests, key = { it.id }) { req ->
                                        val sender = usersMap[req.senderId]
                                        val senderName = sender?.displayName?.ifBlank { sender.username }
                                            ?: req.senderUsername.ifBlank { "Player" }
                                        val senderAvatar = sender?.avatarUrl?.ifBlank { null } ?: req.senderAvatarUrl

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                GamerAvatar(
                                                    avatarUrl = senderAvatar,
                                                    displayName = senderName,
                                                    size = 44,
                                                    onClick = { onOpenProfile(req.senderId) }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { onOpenProfile(req.senderId) }
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(senderName, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        VerificationBadges(
                                                            isOwner = sender?.isOwner == true,
                                                            isVerified = sender?.isVerified == true
                                                        )
                                                    }
                                                    Text(
                                                        text = if (!sender?.username.isNullOrBlank()) "@${sender?.username} wants to connect" else "Sent you a friend request",
                                                        color = PrimeTextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val res = socialRepository.acceptFriendRequest(req.id, req.senderId)
                                                                if (res.isFailure) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to accept request.",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF10B981))
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                socialRepository.rejectFriendRequest(req.id)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(PrimeRed)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (sentRequests.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Sent Requests (${sentRequests.size})",
                                            color = PrimeTextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                                        )
                                    }
                                    items(sentRequests, key = { it.id }) { req ->
                                        val receiver = usersMap[req.receiverId]
                                        val name = receiver?.displayName?.ifBlank { receiver.username } ?: "Player"
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                GamerAvatar(
                                                    avatarUrl = receiver?.avatarUrl ?: "",
                                                    displayName = name,
                                                    size = 42,
                                                    onClick = { onOpenProfile(req.receiverId) }
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { onOpenProfile(req.receiverId) }
                                                ) {
                                                    Text(name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    if (!receiver?.username.isNullOrBlank()) {
                                                        Text("@${receiver?.username} • Pending", color = PrimeTextMuted, fontSize = 12.sp)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            socialRepository.cancelFriendRequest(req.id)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Cancel", color = PrimeTextSecondary, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                    2 -> {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (val fState = friendshipsState) {
                                is UiDataState.Loading -> {
                                    item { PrimeLoadingBox("Loading friends...") }
                                }
                                is UiDataState.Error -> {
                                    item { PrimeErrorRetryCard(fState.message, onRetry = { retryTrigger++ }) }
                                }
                                is UiDataState.Success -> {
                                    val visibleFriends = fState.data.filter { friendship ->
                                        val otherUid = friendship.users.firstOrNull { it != currentUserId } ?: ""
                                        !blockedUserIds.contains(otherUid)
                                    }
                                    if (visibleFriends.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("👥", fontSize = 48.sp)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("No accepted friends yet", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("Accept friend requests to unlock direct messaging.", color = PrimeTextSecondary, fontSize = 13.sp)
                                            }
                                        }
                                    } else {
                                        items(visibleFriends, key = { it.id }) { friendship ->
                                            val friendUid = friendship.users.firstOrNull { it != currentUserId } ?: ""
                                            val friend = usersMap[friendUid]
                                            val friendName = friend?.displayName?.ifBlank { friend.username } ?: "Friend"
                                            val friendHandle = friend?.username ?: ""

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    GamerAvatar(
                                                        avatarUrl = friend?.avatarUrl ?: "",
                                                        displayName = friendName,
                                                        size = 44,
                                                        isOnline = friend?.isOnline == true,
                                                        onClick = if (friendUid.isNotBlank()) {
                                                            { onOpenProfile(friendUid) }
                                                        } else null
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable(enabled = friendUid.isNotBlank()) { onOpenProfile(friendUid) }
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(friendName, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            VerificationBadges(
                                                                isOwner = friend?.isOwner == true,
                                                                isVerified = friend?.isVerified == true
                                                            )
                                                        }
                                                        Text(
                                                            text = if (friendHandle.isNotBlank()) "@$friendHandle • Friends" else "Friends",
                                                            color = Color(0xFF10B981),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val res = socialRepository.getOrCreateConversationWithFriend(friendUid)
                                                                if (res.isSuccess) {
                                                                    activeConversationId = res.getOrThrow()
                                                                    activeChatOtherUserId = friendUid
                                                                    activeChatTitle = if (friendHandle.isNotBlank()) "$friendName (@$friendHandle)" else friendName
                                                                } else {
                                                                    Toast.makeText(
                                                                        context,
                                                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to open chat.",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(onClick = {
                                                        friendToRemove = friendUid to (friendHandle.ifBlank { friendName })
                                                    }) {
                                                        Icon(Icons.Default.PersonRemove, contentDescription = "Unfriend", tint = PrimeRed, modifier = Modifier.size(18.dp))
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
        }
    }

    friendToRemove?.let { (uid, label) ->
        DeleteConfirmationDialog(
            title = "Remove Friend",
            message = "Remove $label from your friends list?",
            isDeleting = isRemovingFriend,
            onConfirm = {
                isRemovingFriend = true
                coroutineScope.launch {
                    socialRepository.removeFriend(uid)
                    isRemovingFriend = false
                    friendToRemove = null
                }
            },
            onDismiss = { friendToRemove = null }
        )
    }
}

@Composable
fun ActiveChatScreen(
    conversationId: String,
    otherUserId: String,
    title: String,
    socialRepository: SocialRepository,
    currentUserId: String,
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember(conversationId) { mutableIntStateOf(0) }
    val messagesState by remember(conversationId, retryTrigger) {
        socialRepository.observeMessagesState(conversationId)
    }.collectAsState(initial = UiDataState.Loading)
    var text by remember(conversationId) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = otherUserId.isNotBlank()) {
                                onOpenProfile(otherUserId)
                            }
                            .testTag("chat_header_profile_button")
                    ) {
                        Text(
                            text = title,
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (otherUserId.isNotBlank()) "Friend-Verified Direct Chat • Tap to view profile" else "Friend-Verified Direct Chat",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (val msgState = messagesState) {
                    is UiDataState.Loading -> {
                        item { PrimeLoadingBox("Loading messages...") }
                    }
                    is UiDataState.Error -> {
                        item { PrimeErrorRetryCard(msgState.message, onRetry = { retryTrigger++ }) }
                    }
                    is UiDataState.Success -> {
                        val messages = msgState.data
                        if (messages.isEmpty()) {
                            item {
                                Text(
                                    text = "No messages yet. Say hello!",
                                    color = PrimeTextMuted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        } else {
                            items(messages, key = { it.id }) { msg ->
                                val isMe = msg.senderId == currentUserId
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                                )
                                            )
                                            .background(if (isMe) PrimeBlue else PrimeCardBackground)
                                            .border(1.dp, if (isMe) PrimeBlue else PrimeBorder, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = if (isMe) Color.White else PrimeTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimeCardBackground)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f).testTag("direct_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            isSending = true
                            coroutineScope.launch {
                                val res = socialRepository.sendDirectMessage(conversationId, text)
                                isSending = false
                                if (res.isSuccess) {
                                    text = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to send message.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    enabled = text.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("send_direct_message_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PrimeBlue)
                }
            }
        }
    }
}
