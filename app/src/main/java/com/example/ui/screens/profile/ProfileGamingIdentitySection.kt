package com.example.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameHandle
import com.example.data.model.RankHistoryEntry
import com.example.data.model.SocialLink
import com.example.data.model.UserProfile
import com.example.data.model.resolvedFavoriteTitles
import com.example.data.repository.AuthRepository
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

enum class GamingIdentitySection(val label: String) {
    HANDLES("Game Handles"),
    TITLES("Favorite Titles"),
    RANKS("Rank History"),
    SOCIALS("Social Links")
}

private val PRESET_HANDLE_PLATFORMS = listOf(
    "BGMI", "Valorant", "Minecraft", "CS2", "Discord", "Steam", "PlayStation", "Xbox", "Free Fire", "COD Mobile"
)

private val PRESET_GAME_TITLES = listOf(
    "BGMI", "Valorant", "Minecraft", "CS2", "Free Fire", "Apex Legends", "Call of Duty", "Fortnite", "Rocket League"
)

private val PRESET_SOCIAL_PLATFORMS = listOf(
    "YouTube", "Twitch", "Discord", "X / Twitter", "Instagram", "Kick", "GitHub", "Website"
)

private fun platformAccentColor(platform: String): Color {
    return when (platform.trim().lowercase()) {
        "valorant", "youtube", "kick" -> PrimeRed
        "bgmi", "free fire", "call of duty", "cod mobile" -> Color(0xFFF59E0B)
        "minecraft" -> Color(0xFF10B981)
        "twitch", "discord", "instagram" -> PrimePurple
        else -> PrimeBlue
    }
}

private fun resolveSafeExternalUrl(platform: String, rawValue: String): String? {
    val trimmed = rawValue.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true)) {
        return trimmed
    }
    val handle = trimmed.removePrefix("@").trim()
    if (handle.isEmpty() || handle.contains(" ")) return null
    return when (platform.trim().lowercase()) {
        "youtube" -> "https://www.youtube.com/@$handle"
        "twitch" -> "https://www.twitch.tv/$handle"
        "x / twitter", "twitter", "x" -> "https://x.com/$handle"
        "instagram" -> "https://www.instagram.com/$handle"
        "kick" -> "https://kick.com/$handle"
        "github" -> "https://github.com/$handle"
        "website" -> if (handle.contains(".")) "https://$handle" else null
        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileHeaderFavoriteTitlesStrip(
    userProfile: UserProfile,
    onOpenSocialLink: (SocialLink) -> Unit
) {
    val titles = remember(userProfile.favoriteTitles, userProfile.favoriteGames) {
        userProfile.resolvedFavoriteTitles()
    }
    val socialLinks = userProfile.socialLinks

    if (titles.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            titles.take(6).forEach { title ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimeCardBackground)
                        .border(1.dp, PrimePurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = PrimePurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        color = PrimeTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }

    if (socialLinks.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            socialLinks.take(4).forEach { link ->
                val accent = platformAccentColor(link.platform)
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clickable { onOpenSocialLink(link) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = link.platform,
                            color = PrimeTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileGamingIdentityShowcase(
    userProfile: UserProfile,
    isOwnProfile: Boolean,
    compactPreview: Boolean = false,
    onEditSection: (GamingIdentitySection) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val titles = remember(userProfile.favoriteTitles, userProfile.favoriteGames) {
        userProfile.resolvedFavoriteTitles()
    }
    val handles = userProfile.gameHandles
    val ranks = userProfile.rankHistory
    val socials = userProfile.socialLinks

    val handleSocialClick: (SocialLink) -> Unit = { link ->
        val safeUrl = resolveSafeExternalUrl(link.platform, link.handleOrUrl)
        if (safeUrl != null) {
            val opened = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
            }.isSuccess
            if (!opened) {
                clipboard.setText(AnnotatedString(link.handleOrUrl))
                Toast.makeText(context, "Copied ${link.platform}: ${link.handleOrUrl}", Toast.LENGTH_SHORT).show()
            }
        } else {
            clipboard.setText(AnnotatedString(link.handleOrUrl))
            Toast.makeText(context, "Copied ${link.platform}: ${link.handleOrUrl}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Game Handles Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_game_handles_card"),
            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentitySectionHeader(
                    icon = Icons.Default.Badge,
                    title = "Game Handles & IDs",
                    accent = PrimeBlue,
                    isOwnProfile = isOwnProfile,
                    editTag = "edit_game_handles_button",
                    onEdit = { onEditSection(GamingIdentitySection.HANDLES) }
                )

                if (handles.isEmpty()) {
                    IdentityEmptyRow(
                        emptyText = "No game handles added yet.",
                        ctaText = "Add Game Handle",
                        isOwnProfile = isOwnProfile,
                        onAdd = { onEditSection(GamingIdentitySection.HANDLES) }
                    )
                } else {
                    val shownHandles = if (compactPreview) handles.take(3) else handles
                    shownHandles.forEach { item ->
                        val accent = platformAccentColor(item.platform)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimeNavySurface)
                                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = item.platform,
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.handle,
                                color = PrimeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(item.handle))
                                    Toast.makeText(context, "Copied ${item.platform} ID", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy ${item.platform} handle",
                                    tint = PrimeBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Favorite Titles Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_favorite_titles_card"),
            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentitySectionHeader(
                    icon = Icons.Default.SportsEsports,
                    title = "Favorite Titles",
                    accent = PrimePurple,
                    isOwnProfile = isOwnProfile,
                    editTag = "edit_favorite_titles_button",
                    onEdit = { onEditSection(GamingIdentitySection.TITLES) }
                )

                if (titles.isEmpty()) {
                    IdentityEmptyRow(
                        emptyText = "No favorite titles added yet.",
                        ctaText = "Add Favorite Titles",
                        isOwnProfile = isOwnProfile,
                        onAdd = { onEditSection(GamingIdentitySection.TITLES) }
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        titles.forEach { game ->
                            val accent = platformAccentColor(game)
                            Surface(
                                color = accent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = game,
                                    color = PrimeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Rank History Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_rank_history_card"),
            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentitySectionHeader(
                    icon = Icons.Default.EmojiEvents,
                    title = "Competitive Rank History",
                    accent = Color(0xFFF59E0B),
                    isOwnProfile = isOwnProfile,
                    editTag = "edit_rank_history_button",
                    onEdit = { onEditSection(GamingIdentitySection.RANKS) }
                )

                if (ranks.isEmpty()) {
                    IdentityEmptyRow(
                        emptyText = "No rank history recorded yet.",
                        ctaText = "Add Rank Record",
                        isOwnProfile = isOwnProfile,
                        onAdd = { onEditSection(GamingIdentitySection.RANKS) }
                    )
                } else {
                    val shownRanks = if (compactPreview) ranks.take(2) else ranks
                    shownRanks.forEachIndexed { idx, entry ->
                        val accent = platformAccentColor(entry.game)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimeNavySurface)
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.game,
                                        color = PrimeTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (entry.season.isNotBlank()) {
                                        Text(
                                            text = entry.season,
                                            color = PrimeTextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = accent.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = entry.rank,
                                            color = accent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (entry.peakRank.isNotBlank()) {
                                        Text(
                                            text = "Peak: ${entry.peakRank}",
                                            color = PrimeTextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (entry.notes.isNotBlank()) {
                                    Text(
                                        text = entry.notes,
                                        color = PrimeTextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                        if (idx < shownRanks.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }

        // 4. Social Media Links Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_social_links_card"),
            colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, PrimeBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentitySectionHeader(
                    icon = Icons.Default.Link,
                    title = "Social Media & Streams",
                    accent = PrimeRed,
                    isOwnProfile = isOwnProfile,
                    editTag = "edit_social_links_button",
                    onEdit = { onEditSection(GamingIdentitySection.SOCIALS) }
                )

                if (socials.isEmpty()) {
                    IdentityEmptyRow(
                        emptyText = "No social media links added yet.",
                        ctaText = "Add Social Link",
                        isOwnProfile = isOwnProfile,
                        onAdd = { onEditSection(GamingIdentitySection.SOCIALS) }
                    )
                } else {
                    val shownSocials = if (compactPreview) socials.take(3) else socials
                    shownSocials.forEach { link ->
                        val accent = platformAccentColor(link.platform)
                        val isOpenable = resolveSafeExternalUrl(link.platform, link.handleOrUrl) != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimeNavySurface)
                                .border(1.dp, PrimeBorder, RoundedCornerShape(10.dp))
                                .clickable { handleSocialClick(link) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = link.platform,
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = link.handleOrUrl,
                                color = PrimeTextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isOpenable) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ContentCopy,
                                contentDescription = if (isOpenable) "Open ${link.platform}" else "Copy ${link.platform}",
                                tint = PrimeBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentitySectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accent: Color,
    isOwnProfile: Boolean,
    editTag: String,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = PrimeTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        if (isOwnProfile) {
            TextButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(editTag)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", color = PrimeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IdentityEmptyRow(
    emptyText: String,
    ctaText: String,
    isOwnProfile: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PrimeNavySurface.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = emptyText,
            color = PrimeTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (isOwnProfile) {
            OutlinedButton(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PrimeBlue.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(ctaText, color = PrimeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GamingIdentityEditorBottomSheet(
    userProfile: UserProfile?,
    initialSection: GamingIdentitySection,
    authRepository: AuthRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeTab by remember(initialSection) { mutableIntStateOf(initialSection.ordinal) }

    val handles = remember(userProfile) {
        mutableStateListOf<GameHandle>().apply {
            addAll(userProfile?.gameHandles.orEmpty())
        }
    }
    val favoriteTitles = remember(userProfile) {
        mutableStateListOf<String>().apply {
            addAll(userProfile?.resolvedFavoriteTitles().orEmpty())
        }
    }
    val rankHistory = remember(userProfile) {
        mutableStateListOf<RankHistoryEntry>().apply {
            addAll(userProfile?.rankHistory.orEmpty())
        }
    }
    val socialLinks = remember(userProfile) {
        mutableStateListOf<SocialLink>().apply {
            addAll(userProfile?.socialLinks.orEmpty())
        }
    }

    // Input draft states
    var handlePlatform by remember { mutableStateOf("BGMI") }
    var handleValue by remember { mutableStateOf("") }

    var customTitleInput by remember { mutableStateOf("") }

    var rankGame by remember { mutableStateOf("Valorant") }
    var rankSeason by remember { mutableStateOf("") }
    var rankTier by remember { mutableStateOf("") }
    var peakRank by remember { mutableStateOf("") }
    var rankNotes by remember { mutableStateOf("") }

    var socialPlatform by remember { mutableStateOf("YouTube") }
    var socialValue by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PrimeTextPrimary,
        unfocusedTextColor = PrimeTextPrimary,
        focusedBorderColor = PrimeBlue,
        unfocusedBorderColor = PrimeBorder,
        focusedContainerColor = PrimeNavySurface,
        unfocusedContainerColor = PrimeNavySurface
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = PrimeCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Gaming Identity & Social Links",
                color = PrimeTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Customize the handles, favorite games, competitive ranks, and links shown on your profile.",
                color = PrimeTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = PrimeNavySurface,
                contentColor = PrimeBlue,
                edgePadding = 4.dp
            ) {
                GamingIdentitySection.entries.forEachIndexed { idx, section ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = {
                            Text(
                                text = section.label,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == idx) PrimeBlue else PrimeTextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (GamingIdentitySection.entries[activeTab]) {
                GamingIdentitySection.HANDLES -> {
                    Text("Platform / Game", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PRESET_HANDLE_PLATFORMS.forEach { preset ->
                            FilterChip(
                                selected = handlePlatform == preset,
                                onClick = { handlePlatform = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimeBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = PrimeTextPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = handlePlatform,
                        onValueChange = { handlePlatform = it.take(30) },
                        label = { Text("Game or Platform") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("handle_platform_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = handleValue,
                        onValueChange = { handleValue = it.take(50) },
                        label = { Text("In-Game Name / Player ID / Tag") },
                        placeholder = { Text("e.g. ShadowStrike#777 or 5192837412") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("handle_value_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val p = handlePlatform.trim()
                            val h = handleValue.trim()
                            if (p.isNotEmpty() && h.isNotEmpty() && handles.size < 15) {
                                handles.removeAll { it.platform.equals(p, ignoreCase = true) && it.handle.equals(h, ignoreCase = true) }
                                handles.add(GameHandle(p, h))
                                handleValue = ""
                            }
                        },
                        enabled = handlePlatform.isNotBlank() && handleValue.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_handle_entry_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimeBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Game Handle", color = PrimeBlue, fontWeight = FontWeight.Bold)
                    }

                    if (handles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = PrimeBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        handles.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimeNavySurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.platform}: ${item.handle}",
                                    color = PrimeTextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { handles.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove handle", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                GamingIdentitySection.TITLES -> {
                    Text("Quick-Pick Popular Titles", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PRESET_GAME_TITLES.forEach { preset ->
                            val selected = favoriteTitles.any { it.equals(preset, ignoreCase = true) }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        favoriteTitles.removeAll { it.equals(preset, ignoreCase = true) }
                                    } else if (favoriteTitles.size < 20) {
                                        favoriteTitles.add(preset)
                                    }
                                },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimePurple.copy(alpha = 0.25f),
                                    selectedLabelColor = PrimeTextPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTitleInput,
                            onValueChange = { customTitleInput = it.take(40) },
                            label = { Text("Add Custom Title") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("favorite_title_input"),
                            colors = fieldColors
                        )
                        Button(
                            onClick = {
                                val clean = customTitleInput.trim()
                                if (clean.isNotEmpty() && favoriteTitles.none { it.equals(clean, ignoreCase = true) } && favoriteTitles.size < 20) {
                                    favoriteTitles.add(clean)
                                    customTitleInput = ""
                                }
                            },
                            enabled = customTitleInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimePurple),
                            modifier = Modifier.heightIn(min = 52.dp).testTag("add_favorite_title_button")
                        ) {
                            Text("Add", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (favoriteTitles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            favoriteTitles.forEachIndexed { idx, title ->
                                Surface(
                                    color = PrimeNavySurface,
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, PrimePurple.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                    ) {
                                        Text(title, color = PrimeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { favoriteTitles.removeAt(idx) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove $title", tint = PrimeRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GamingIdentitySection.RANKS -> {
                    OutlinedTextField(
                        value = rankGame,
                        onValueChange = { rankGame = it.take(40) },
                        label = { Text("Game Title *") },
                        placeholder = { Text("e.g. Valorant, BGMI, CS2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rank_game_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rankSeason,
                            onValueChange = { rankSeason = it.take(40) },
                            label = { Text("Season / Act") },
                            placeholder = { Text("e.g. Ep 9 Act 2") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("rank_season_input"),
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = rankTier,
                            onValueChange = { rankTier = it.take(40) },
                            label = { Text("Rank Achieved *") },
                            placeholder = { Text("e.g. Conqueror / Immortal I") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("rank_tier_input"),
                            colors = fieldColors
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = peakRank,
                        onValueChange = { peakRank = it.take(40) },
                        label = { Text("Peak Rank / Rating (Optional)") },
                        placeholder = { Text("e.g. Radiant #412 / 2450 ELO") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rank_peak_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rankNotes,
                        onValueChange = { rankNotes = it.take(120) },
                        label = { Text("Role / Highlights (Optional)") },
                        placeholder = { Text("e.g. IGL / Top Fragger • 64% Win Rate") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rank_notes_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val g = rankGame.trim()
                            val r = rankTier.trim()
                            if (g.isNotEmpty() && r.isNotEmpty() && rankHistory.size < 25) {
                                rankHistory.add(
                                    0,
                                    RankHistoryEntry(
                                        game = g,
                                        season = rankSeason.trim(),
                                        rank = r,
                                        peakRank = peakRank.trim(),
                                        notes = rankNotes.trim()
                                    )
                                )
                                rankSeason = ""
                                rankTier = ""
                                peakRank = ""
                                rankNotes = ""
                            }
                        },
                        enabled = rankGame.isNotBlank() && rankTier.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_rank_entry_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Rank Record", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }

                    if (rankHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = PrimeBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        rankHistory.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimeNavySurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${entry.game} • ${entry.rank}",
                                        color = PrimeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val sub = listOfNotNull(
                                        entry.season.takeIf { it.isNotBlank() },
                                        entry.peakRank.takeIf { it.isNotBlank() }?.let { "Peak: $it" }
                                    ).joinToString(" • ")
                                    if (sub.isNotBlank()) {
                                        Text(sub, color = PrimeTextSecondary, fontSize = 11.sp)
                                    }
                                }
                                IconButton(onClick = { rankHistory.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove rank", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                GamingIdentitySection.SOCIALS -> {
                    Text("Platform", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PRESET_SOCIAL_PLATFORMS.forEach { preset ->
                            FilterChip(
                                selected = socialPlatform == preset,
                                onClick = { socialPlatform = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimeRed.copy(alpha = 0.25f),
                                    selectedLabelColor = PrimeTextPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = socialValue,
                        onValueChange = { socialValue = it.take(200) },
                        label = { Text("Profile URL or Handle *") },
                        placeholder = { Text("e.g. https://youtube.com/@handle or @handle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("social_link_value_input"),
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val p = socialPlatform.trim()
                            val v = socialValue.trim()
                            if (p.isNotEmpty() && v.isNotEmpty() && socialLinks.size < 12) {
                                socialLinks.removeAll { it.platform.equals(p, ignoreCase = true) }
                                socialLinks.add(SocialLink(p, v))
                                socialValue = ""
                            }
                        },
                        enabled = socialPlatform.isNotBlank() && socialValue.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_social_link_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimeRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Social Link", color = PrimeRed, fontWeight = FontWeight.Bold)
                    }

                    if (socialLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = PrimeBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        socialLinks.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimeNavySurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.platform}: ${item.handleOrUrl}",
                                    color = PrimeTextPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { socialLinks.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove link", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (saveError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = saveError ?: "", color = PrimeRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    isSaving = true
                    saveError = null
                    coroutineScope.launch {
                        val res = authRepository.updateGamingIdentity(
                            favoriteTitles = favoriteTitles.toList(),
                            gameHandles = handles.toList(),
                            rankHistory = rankHistory.toList(),
                            socialLinks = socialLinks.toList()
                        )
                        isSaving = false
                        if (res.isSuccess) {
                            onSaved()
                        } else {
                            saveError = res.exceptionOrNull()?.localizedMessage ?: "Failed to save gaming profile."
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_gaming_identity_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Gaming Profile", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
