package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.SafetyRepository
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
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    authRepository: AuthRepository,
    safetyRepository: SafetyRepository,
    currentUserId: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToTournament: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    isHindi: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val notificationsState by remember(retryTrigger) {
        safetyRepository.observeNotificationsState()
    }.collectAsState(initial = UiDataState.Loading)
    val blockedUserIds by safetyRepository.observeBlockedUserIds().collectAsState(initial = emptySet())
    val rawNotifications = (notificationsState as? UiDataState.Success)?.data ?: emptyList()
    val notifications = remember(rawNotifications, blockedUserIds) {
        rawNotifications.filter { !blockedUserIds.contains(it.senderId) }
    }
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        if (isHindi) "सभी" else "All",
        if (isHindi) "सोशल" else "Social",
        if (isHindi) "टूर्नामेंट्स" else "Events"
    )

    val filteredNotifications = remember(selectedTab, notifications) {
        when (selectedTab) {
            1 -> notifications.filter {
                it.type in listOf("follow", "friend_request", "friend_accepted", "like", "comment", "message")
            }
            2 -> notifications.filter {
                it.type in listOf("tournament", "community_invite", "moderation")
            }
            else -> notifications
        }
    }

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back_button")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                            }
                            Column {
                                Text(
                                    text = if (isHindi) "सूचनाएं" else "Notifications",
                                    color = PrimeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${notifications.count { !it.isRead }} unread alerts",
                                    color = PrimeTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    safetyRepository.markAllNotificationsRead()
                                }
                            },
                            modifier = Modifier.testTag("notifications_mark_all_read")
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = PrimeBlue,
                                modifier = Modifier.size(22.dp)
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
                                height = 2.dp
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
                                },
                                modifier = Modifier.testTag("notification_tab_$index")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                when (val currentState = notificationsState) {
                    is UiDataState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PrimeLoadingBox(
                                message = if (isHindi) "सूचनाएं लोड हो रही हैं..." else "Loading notifications..."
                            )
                        }
                    }
                    is UiDataState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PrimeErrorRetryCard(
                                message = currentState.message,
                                onRetry = { retryTrigger++ }
                            )
                        }
                    }
                    is UiDataState.Success -> if (filteredNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = PrimeTextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isHindi) "अभी तक कोई सूचना नहीं है।" else "No notifications yet.",
                                    color = PrimeTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi) "फ्रेंड रिक्वेस्ट, फॉलो, लाइक और टूर्नामेंट अलर्ट यहां दिखाई देंगे।" else "Friend requests, follows, likes, comments, and tournament updates appear here.",
                                    color = PrimeTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotifications, key = { it.id }) { item ->
                                val sender = usersMap[item.senderId]
                                NotificationCard(
                                    item = item,
                                    senderProfile = sender,
                                    onClick = {
                                        coroutineScope.launch {
                                            if (!item.isRead) {
                                                safetyRepository.markNotificationRead(item.id)
                                            }
                                        }
                                        when (item.type) {
                                            "tournament" -> onNavigateToTournament()
                                            "community_invite" -> onNavigateToCommunity()
                                            "friend_request", "friend_accepted", "message" -> onNavigateToMessages()
                                            "follow", "like", "comment", "moderation" -> {
                                                val uid = item.senderId.ifBlank { item.targetId }
                                                if (uid.isNotBlank()) onNavigateToProfile(uid)
                                            }
                                            else -> {
                                                val uid = item.senderId.ifBlank { item.targetId }
                                                if (uid.isNotBlank()) onNavigateToProfile(uid)
                                            }
                                        }
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            safetyRepository.deleteNotification(item.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    senderProfile: UserProfile?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, accentColor) = when (item.type) {
        "tournament" -> Icons.Default.EmojiEvents to PrimeBlue
        "friend_request", "friend_accepted", "follow" -> Icons.Default.PersonAdd to PrimePurple
        "like" -> Icons.Default.Favorite to PrimeRed
        "comment" -> Icons.Default.ChatBubble to PrimeBlue
        "message" -> Icons.AutoMirrored.Filled.Chat to Color(0xFF10B981)
        "community_invite" -> Icons.Default.Groups to PrimePurple
        "moderation" -> Icons.Default.VerifiedUser to PrimeBlue
        else -> Icons.Default.Notifications to Color(0xFFF59E0B)
    }

    val senderName = senderProfile?.username?.ifBlank { null } ?: item.senderName
    val senderAvatar = senderProfile?.avatarUrl?.ifBlank { null } ?: item.senderAvatarUrl

    Surface(
        color = if (item.isRead) PrimeCardBackground else PrimeNavySurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (item.isRead) PrimeBorder else PrimeBlue.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .testTag("notification_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (senderAvatar.isNotBlank()) {
                GamerAvatar(avatarUrl = senderAvatar, displayName = senderName, size = 40)
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (senderName.isNotBlank()) "@$senderName • ${item.title}" else item.title,
                            color = PrimeTextPrimary,
                            fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        VerificationBadges(
                            isOwner = senderProfile?.isOwner == true,
                            isVerified = senderProfile?.isVerified == true
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatRelativeTime(item.createdAt),
                        color = PrimeTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.body,
                    color = PrimeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimeBlue)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete notification", tint = PrimeTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
