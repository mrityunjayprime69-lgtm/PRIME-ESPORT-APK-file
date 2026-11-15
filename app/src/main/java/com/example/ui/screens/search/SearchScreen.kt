package com.example.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelVideo
import com.example.data.model.UiDataState
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.EsportRepository
import com.example.data.repository.MinecraftRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.ui.components.GamerAvatar
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.VerificationBadges
import com.example.ui.components.VideoPlayerModal
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    creatorRepository: CreatorRepository,
    esportRepository: EsportRepository,
    minecraftRepository: MinecraftRepository,
    safetyRepository: SafetyRepository? = null,
    currentUserId: String = "",
    onBack: () -> Unit,
    onSelectPlayer: (String) -> Unit = {},
    onSelectCommunity: () -> Unit = {},
    onSelectTournament: () -> Unit = {},
    onSelectMinecraft: () -> Unit = {},
    isHindi: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Players", "Videos", "Tournaments", "Communities", "Minecraft", "Posts")
    var playingVideo by remember { mutableStateOf<ChannelVideo?>(null) }

    val quickGameFilters = listOf("BGMI", "Minecraft", "Free Fire", "Valorant", "COD Mobile")

    var retryTrigger by remember { mutableIntStateOf(0) }
    val allUsersState by remember(retryTrigger) {
        authRepository.observeAllUsersState()
    }.collectAsState(initial = UiDataState.Loading)
    val allVideosState by remember(retryTrigger) {
        creatorRepository.observeAllPublishedVideosState()
    }.collectAsState(initial = UiDataState.Loading)
    val postsState by remember(retryTrigger) {
        socialRepository.observePostsState()
    }.collectAsState(initial = UiDataState.Loading)
    val tournamentsState by remember(retryTrigger) {
        esportRepository.observeTournamentsState()
    }.collectAsState(initial = UiDataState.Loading)
    val communitiesState by remember(retryTrigger) {
        esportRepository.observeCommunitiesState()
    }.collectAsState(initial = UiDataState.Loading)
    val minecraftState by remember(retryTrigger) {
        minecraftRepository.observeMinecraftServersState()
    }.collectAsState(initial = UiDataState.Loading)

    val blockedUserIds by remember(safetyRepository) {
        safetyRepository?.observeBlockedUserIds() ?: flowOf(emptySet())
    }.collectAsState(initial = emptySet())

    val allUsers = remember(allUsersState, blockedUserIds) {
        ((allUsersState as? UiDataState.Success)?.data ?: emptyList())
            .filter { !blockedUserIds.contains(it.userId) }
    }
    val allVideos = remember(allVideosState, blockedUserIds) {
        ((allVideosState as? UiDataState.Success)?.data ?: emptyList())
            .filter { !blockedUserIds.contains(it.ownerId) }
    }
    val posts = remember(postsState, blockedUserIds) {
        ((postsState as? UiDataState.Success)?.data ?: emptyList())
            .filter { !blockedUserIds.contains(it.authorId) }
    }
    val tournaments = (tournamentsState as? UiDataState.Success)?.data ?: emptyList()
    val communities = (communitiesState as? UiDataState.Success)?.data ?: emptyList()
    val minecraftServers = (minecraftState as? UiDataState.Success)?.data ?: emptyList()

    val isLoadingAny = allUsersState is UiDataState.Loading &&
        allVideosState is UiDataState.Loading &&
        postsState is UiDataState.Loading &&
        tournamentsState is UiDataState.Loading &&
        communitiesState is UiDataState.Loading &&
        minecraftState is UiDataState.Loading

    val firstError = listOf(
        allUsersState,
        allVideosState,
        postsState,
        tournamentsState,
        communitiesState,
        minecraftState
    ).filterIsInstance<UiDataState.Error>().firstOrNull()

    val query = searchQuery.trim().lowercase()

    val filteredPlayers = remember(query, allUsers) {
        if (query.isEmpty()) emptyList()
        else allUsers.filter {
            it.username.lowercase().contains(query) ||
                it.displayName.lowercase().contains(query) ||
                it.favoriteGames.lowercase().contains(query)
        }
    }

    val filteredVideos = remember(query, allVideos) {
        if (query.isEmpty()) emptyList()
        else allVideos.filter {
            it.title.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.authorUsername.lowercase().contains(query)
        }
    }

    val filteredPosts = remember(query, posts) {
        if (query.isEmpty()) emptyList()
        else posts.filter {
            it.content.lowercase().contains(query) ||
                it.gameTag.lowercase().contains(query) ||
                it.authorUsername.lowercase().contains(query)
        }
    }

    val filteredTournaments = remember(query, tournaments) {
        if (query.isEmpty()) emptyList()
        else tournaments.filter {
            it.title.lowercase().contains(query) ||
                it.game.lowercase().contains(query)
        }
    }

    val filteredCommunities = remember(query, communities) {
        if (query.isEmpty()) emptyList()
        else communities.filter {
            it.name.lowercase().contains(query) ||
                it.description.lowercase().contains(query)
        }
    }

    val filteredMinecraft = remember(query, minecraftServers) {
        if (query.isEmpty()) emptyList()
        else minecraftServers.filter {
            it.name.lowercase().contains(query) ||
                it.address.lowercase().contains(query) ||
                it.edition.lowercase().contains(query)
        }
    }

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    if (isHindi) "खिलाड़ी, वीडियो, टूर्नामेंट खोजें..." else "Search players, videos, tournaments...",
                                    color = PrimeTextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = PrimeTextSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PrimeTextMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PrimeCardBackground,
                                unfocusedContainerColor = PrimeCardBackground,
                                focusedBorderColor = PrimeBlue,
                                unfocusedBorderColor = PrimeBorder,
                                focusedTextColor = PrimeTextPrimary,
                                unfocusedTextColor = PrimeTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("search_input_field")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimeBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = PrimeCardBackground,
                                    labelColor = PrimeTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (selectedFilter == filter) PrimeBlue else PrimeBorder,
                                    selectedBorderColor = PrimeBlue,
                                    enabled = true,
                                    selected = selectedFilter == filter
                                ),
                                modifier = Modifier.testTag("search_filter_$filter")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (searchQuery.isBlank()) {
                item {
                    Column {
                        Text(
                            text = if (isHindi) "गेम द्वारा खोजें" else "Quick Game Filters",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickGameFilters.forEach { gameTag ->
                                Surface(
                                    color = PrimeNavySurface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { searchQuery = gameTag }
                                        .border(1.dp, PrimeBorder, RoundedCornerShape(8.dp))
                                        .testTag("quick_filter_$gameTag")
                                ) {
                                    Text(
                                        text = gameTag,
                                        color = PrimeBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isLoadingAny) {
                item {
                    PrimeLoadingBox(
                        message = if (isHindi) "खोज परिणाम लोड हो रहे हैं..." else "Searching PRIME ESPORT..."
                    )
                }
            } else if (firstError != null && allUsers.isEmpty() && allVideos.isEmpty() && posts.isEmpty() && tournaments.isEmpty() && communities.isEmpty() && minecraftServers.isEmpty()) {
                item {
                    PrimeErrorRetryCard(
                        message = firstError.message,
                        onRetry = { retryTrigger++ }
                    )
                }
            } else {
                // Players Results
                if (selectedFilter in listOf("All", "Players") && filteredPlayers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Players (${filteredPlayers.size})",
                            color = PrimeBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredPlayers, key = { "player_${it.userId}" }) { user ->
                        val displayName = user.displayName.ifBlank { user.username }
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlayer(user.userId) }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_player_${user.userId}")
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                GamerAvatar(
                                    avatarUrl = user.avatarUrl,
                                    displayName = displayName,
                                    size = 42
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(displayName, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        VerificationBadges(isOwner = user.isOwner, isVerified = user.isVerified)
                                    }
                                    Text("@${user.username}", color = PrimeTextMuted, fontSize = 12.sp)
                                    if (user.favoriteGames.isNotBlank()) {
                                        Text(user.favoriteGames, color = PrimePurple, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // Videos Results
                if (selectedFilter in listOf("All", "Videos") && filteredVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Videos (${filteredVideos.size})",
                            color = PrimePurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredVideos, key = { "video_${it.id}" }) { video ->
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playingVideo = video }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_video_${video.id}")
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimeNavySurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (video.thumbnailUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                    Text("@${video.authorUsername} • ${video.category}", color = PrimeTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Tournaments Results
                if (selectedFilter in listOf("All", "Tournaments") && filteredTournaments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tournaments (${filteredTournaments.size})",
                            color = PrimeBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredTournaments, key = { "tour_${it.id}" }) { tournament ->
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTournament() }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_tournament_${tournament.id}")
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimeBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tournament.title, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${tournament.game} • ${tournament.prizePool}", color = PrimeTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Communities Results
                if (selectedFilter in listOf("All", "Communities") && filteredCommunities.isNotEmpty()) {
                    item {
                        Text(
                            text = "Communities (${filteredCommunities.size})",
                            color = PrimePurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredCommunities, key = { "comm_${it.id}" }) { community ->
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCommunity() }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_community_${community.id}")
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimePurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = PrimePurple, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(community.name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${community.memberCount} members • ${community.description}", color = PrimeTextSecondary, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Minecraft Results
                if (selectedFilter in listOf("All", "Minecraft") && filteredMinecraft.isNotEmpty()) {
                    item {
                        Text(
                            text = "Minecraft Servers (${filteredMinecraft.size})",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredMinecraft, key = { "mc_${it.id}" }) { server ->
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMinecraft() }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_minecraft_${server.id}")
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Games, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server.name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${server.address} • ${server.edition} ${server.version}", color = PrimeTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Posts Results
                if (selectedFilter in listOf("All", "Posts") && filteredPosts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Posts (${filteredPosts.size})",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    items(filteredPosts, key = { "post_${it.id}" }) { post ->
                        Surface(
                            color = PrimeCardBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (post.authorId.isNotBlank()) onSelectPlayer(post.authorId) }
                                .border(1.dp, PrimeBorder, RoundedCornerShape(12.dp))
                                .testTag("search_result_post_${post.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("@${post.authorUsername}", color = PrimeBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(post.content, color = PrimeTextPrimary, fontSize = 14.sp, maxLines = 2)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("#${post.gameTag} • ${post.likesCount} likes", color = PrimeTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                val hasAny = when (selectedFilter) {
                    "Players" -> filteredPlayers.isNotEmpty()
                    "Videos" -> filteredVideos.isNotEmpty()
                    "Tournaments" -> filteredTournaments.isNotEmpty()
                    "Communities" -> filteredCommunities.isNotEmpty()
                    "Minecraft" -> filteredMinecraft.isNotEmpty()
                    "Posts" -> filteredPosts.isNotEmpty()
                    else -> filteredPlayers.isNotEmpty() || filteredVideos.isNotEmpty() ||
                        filteredTournaments.isNotEmpty() || filteredCommunities.isNotEmpty() ||
                        filteredMinecraft.isNotEmpty() || filteredPosts.isNotEmpty()
                }
                if (!hasAny) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isHindi) "'$searchQuery' के लिए कोई परिणाम नहीं मिला" else "No results found for '$searchQuery'",
                                color = PrimeTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    playingVideo?.let { video ->
        VideoPlayerModal(
            video = video,
            creatorProfile = allUsers.firstOrNull { it.userId == video.ownerId },
            isOwner = currentUserId.isNotBlank() && video.ownerId == currentUserId,
            onDismiss = { playingVideo = null },
            onOpenProfile = { uid ->
                playingVideo = null
                onSelectPlayer(uid)
            }
        )
    }
}
