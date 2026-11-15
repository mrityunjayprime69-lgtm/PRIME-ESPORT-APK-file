package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import com.google.firebase.Timestamp

@Composable
fun PrimeLoadingBox(
    message: String = "Loading...",
    modifier: Modifier = Modifier,
    accentColor: Color = PrimeBlue
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .testTag("loading_indicator"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = accentColor,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
        )
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = PrimeTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PrimeErrorRetryCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimeBlue
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp)
            .testTag("error_retry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = PrimeRed,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message.ifBlank { "Unable to load data right now." },
                color = PrimeTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("retry_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class NavTab(val titleEn: String, val titleHi: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", "होम", Icons.Filled.Home, Icons.Outlined.Home),
    REELS("Reels", "रील्स", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    COMMUNITIES("Communities", "कम्युनिटीज़", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    TOURNAMENTS("Tournaments", "टूर्नामेंट्स", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    PROFILE("Profile", "प्रोफाइल", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun VerificationBadges(
    isOwner: Boolean,
    isVerified: Boolean,
    showOwnerPill: Boolean = false
) {
    if (isVerified) {
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Verified Account",
            tint = PrimeBlue,
            modifier = Modifier.size(15.dp)
        )
    }
    if (isOwner) {
        Spacer(modifier = Modifier.width(4.dp))
        if (showOwnerPill) {
            Surface(
                color = PrimePurple.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimePurple.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Official Owner",
                        tint = PrimePurple,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Official Owner",
                        color = PrimePurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Official Owner",
                tint = PrimePurple,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val diffMs = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
    val seconds = diffMs / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        seconds < 45L -> "Just now"
        minutes < 60L -> "${minutes}m ago"
        hours < 24L -> "${hours}h ago"
        days < 7L -> "${days}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            sdf.format(timestamp.toDate())
        }
    }
}

@Composable
fun PrimeTopAppBar(
    title: String = "PRIME ESPORT",
    isHindi: Boolean = false,
    unreadMessagesCount: Int = 0,
    unreadNotificationsCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMinecraftHubClick: () -> Unit = {},
    onCreatorStudioClick: () -> Unit = {}
) {
    Surface(
        color = PrimeDarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMinecraftHubClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(PrimeBlue, PrimePurple, PrimeRed))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PRIME ",
                                color = PrimeTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "ESPORT",
                                color = PrimeRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (isHindi) "खेलें • जुड़ें • जीतें" else "PLAY • CONNECT • COMPETE",
                            color = PrimeTextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUploadClick,
                        modifier = Modifier.testTag("top_bar_upload")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimeBlue, PrimePurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create / Upload",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("top_bar_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimeTextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = onCreatorStudioClick,
                        modifier = Modifier.testTag("top_bar_creator_studio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Creator Studio",
                            tint = PrimeBlue,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(containerColor = PrimeRed) {
                                    Text(if (unreadNotificationsCount > 99) "99+" else "$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier.testTag("top_bar_notifications")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = PrimeTextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    BadgedBox(
                        badge = {
                            if (unreadMessagesCount > 0) {
                                Badge(containerColor = PrimeBlue) {
                                    Text(if (unreadMessagesCount > 99) "99+" else "$unreadMessagesCount")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onMessagesClick,
                            modifier = Modifier.testTag("top_bar_messages")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Direct Messages",
                                tint = PrimeTextSecondary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrimeBottomNavigation(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    isHindi: Boolean = false
) {
    Surface(
        color = PrimeNavySurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            NavigationBar(
                containerColor = PrimeNavySurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .height(68.dp)
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.name,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (isHindi) tab.titleHi else tab.titleEn,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimeBlue,
                            selectedTextColor = PrimeBlue,
                            unselectedIconColor = PrimeTextSecondary,
                            unselectedTextColor = PrimeTextMuted,
                            indicatorColor = PrimeBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun GamerAvatar(
    avatarUrl: String,
    displayName: String,
    size: Int = 42,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, PrimeBlue, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimeNavySurface, PrimeBlue.copy(alpha = 0.6f)))
                    )
                    .border(1.5.dp, PrimeBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase().ifBlank { "P" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.42).sp
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .size((size * 0.28).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(1.5.dp, PrimeDarkBackground, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}
