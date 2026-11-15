package com.example.ui.screens.communities

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Community
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.EsportRepository
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
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    esportRepository: EsportRepository,
    currentUserId: String,
    isOfficialOwner: Boolean = false,
    isHindi: Boolean = false,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val communitiesState by remember(retryTrigger) {
        esportRepository.observeCommunitiesState()
    }.collectAsState(initial = UiDataState.Loading)
    val communities = (communitiesState as? UiDataState.Success)?.data ?: emptyList()
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())

    var selectedCommunityId by remember { mutableStateOf<String?>(null) }
    val selectedCommunity = communities.find { it.id == selectedCommunityId }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }

    var communityPendingDelete by remember { mutableStateOf<Community?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    if (selectedCommunity != null) {
        CommunityDetailScreen(
            community = selectedCommunity,
            usersMap = usersMap,
            socialRepository = socialRepository,
            esportRepository = esportRepository,
            currentUserId = currentUserId,
            isOfficialOwner = isOfficialOwner,
            onBack = { selectedCommunityId = null },
            onDeleteCommunity = {
                deleteError = null
                communityPendingDelete = selectedCommunity
            },
            onOpenProfile = onOpenProfile
        )
    } else {
        Scaffold(
            containerColor = PrimeDarkBackground,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateCommunityDialog = true },
                    containerColor = PrimeBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("create_community_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Community")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    item {
                        Text(
                            text = if (isHindi) "गेमिंग कम्युनिटीज़" else "PRIME COMMUNITIES",
                            color = PrimeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        Text(
                            text = if (isHindi) "अपने पसंदीदा गेमर्स और सर्वर से जुड़ें" else "Gaming hubs & squad servers. Chat, share scrims, and form clans.",
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    when (val state = communitiesState) {
                        is UiDataState.Loading -> {
                            item {
                                PrimeLoadingBox(
                                    message = if (isHindi) "कम्युनिटीज़ लोड हो रही हैं..." else "Loading communities..."
                                )
                            }
                        }
                        is UiDataState.Error -> {
                            item {
                                PrimeErrorRetryCard(
                                    message = state.message,
                                    onRetry = { retryTrigger++ }
                                )
                            }
                        }
                        is UiDataState.Success -> {
                            if (state.data.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🎮", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (isHindi) "अभी कोई कम्युनिटी नहीं है" else "No communities available yet.",
                                            color = PrimeTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (isHindi) "अपना गेमिंग क्लान या सर्वर बनाने वाले पहले व्यक्ति बनें!" else "Create a gaming community or clan hub for your squad!",
                                            color = PrimeTextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                                        )
                                        Button(
                                            onClick = { showCreateCommunityDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Create Community", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                items(state.data, key = { it.id }) { community ->
                                    val canDelete = community.ownerId == currentUserId || isOfficialOwner
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable { selectedCommunityId = community.id }
                                            .testTag("community_card_${community.id}"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimeNavySurface)
                                                    .border(1.5.dp, PrimeBlue, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = community.name.take(1).uppercase().ifBlank { "C" },
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimeBlue,
                                                    fontSize = 20.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(community.name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                if (community.description.isNotBlank()) {
                                                    Text(community.description, color = PrimeTextSecondary, fontSize = 12.sp, maxLines = 1)
                                                }
                                                Text("${community.memberCount} Members", color = PrimePurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            if (canDelete) {
                                                IconButton(
                                                    onClick = {
                                                        deleteError = null
                                                        communityPendingDelete = community
                                                    },
                                                    modifier = Modifier.size(34.dp).testTag("delete_community_${community.id}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Community", tint = PrimeRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Button(
                                                onClick = { selectedCommunityId = community.id },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimeNavySurface),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag("open_community_${community.id}")
                                            ) {
                                                Text("Open", color = PrimeBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

    if (showCreateCommunityDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateCommunityDialog = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            var name by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var rules by remember { mutableStateOf("") }
            var isCreating by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Create Gaming Community",
                    color = PrimeTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Community Name") },
                    placeholder = { Text("Enter community or clan name") },
                    modifier = Modifier.fillMaxWidth().testTag("community_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("What is this community about?") },
                    modifier = Modifier.fillMaxWidth().testTag("community_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Server Rules (Optional)") },
                    placeholder = { Text("Add community guidelines and rules") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimePurple,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(errorMessage ?: "", color = PrimeRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isCreating || name.isBlank()) return@Button
                        isCreating = true
                        errorMessage = null
                        coroutineScope.launch {
                            val res = esportRepository.createCommunity(
                                name = name,
                                description = description,
                                iconUrl = "",
                                rules = rules.ifBlank { "Be respectful and play fair." }
                            )
                            isCreating = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Community created!", Toast.LENGTH_SHORT).show()
                                showCreateCommunityDialog = false
                            } else {
                                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to create community."
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_community_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Create Server", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    communityPendingDelete?.let { community ->
        DeleteConfirmationDialog(
            title = "Delete Community",
            message = "Delete '${community.name}'? This removes the community hub.",
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onConfirm = {
                isDeleting = true
                deleteError = null
                coroutineScope.launch {
                    val res = esportRepository.deleteCommunity(community.id)
                    isDeleting = false
                    if (res.isSuccess) {
                        if (selectedCommunityId == community.id) {
                            selectedCommunityId = null
                        }
                        communityPendingDelete = null
                        Toast.makeText(context, "Community deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        deleteError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete community."
                    }
                }
            },
            onDismiss = { communityPendingDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    community: Community,
    usersMap: Map<String, UserProfile>,
    socialRepository: SocialRepository,
    esportRepository: EsportRepository,
    currentUserId: String,
    isOfficialOwner: Boolean = false,
    onBack: () -> Unit,
    onDeleteCommunity: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val channels by esportRepository.observeCommunityChannels(community.id).collectAsState(initial = emptyList())
    val members by esportRepository.observeCommunityMembers(community.id).collectAsState(initial = emptyList())
    val friendships by socialRepository.observeFriendships().collectAsState(initial = emptyList())

    val isMember = members.any { it.userId == currentUserId }
    val channelOptions = remember(channels) {
        if (channels.isEmpty()) {
            listOf("general" to "general-chat")
        } else {
            channels.map { it.id to it.name.ifBlank { it.id } }
        }
    }
    var selectedChannelId by remember(community.id) { mutableStateOf("general") }
    LaunchedEffect(channelOptions) {
        if (channelOptions.none { it.first == selectedChannelId }) {
            selectedChannelId = channelOptions.first().first
        }
    }
    val selectedChannelName = channelOptions.firstOrNull { it.first == selectedChannelId }?.second ?: selectedChannelId

    var messagesRetryTrigger by remember(community.id, selectedChannelId) { mutableIntStateOf(0) }
    val messagesState by remember(community.id, selectedChannelId, messagesRetryTrigger) {
        esportRepository.observeCommunityMessagesState(community.id, selectedChannelId)
    }.collectAsState(initial = UiDataState.Loading)
    var messageText by remember(community.id) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PrimeDarkBackground,
        topBar = {
            Surface(color = PrimeNavySurface, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimeTextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(community.name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("#$selectedChannelName • ${community.memberCount} members", color = PrimeBlue, fontSize = 12.sp)
                    }
                    if (!isMember) {
                        Button(
                            onClick = {
                                isJoining = true
                                coroutineScope.launch {
                                    val res = esportRepository.joinCommunity(community.id)
                                    isJoining = false
                                    if (res.isFailure) {
                                        Toast.makeText(
                                            context,
                                            res.exceptionOrNull()?.localizedMessage ?: "Failed to join community.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isJoining,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("join_community_button")
                        ) {
                            Text(if (isJoining) "..." else "Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(
                        onClick = { showInviteSheet = true },
                        modifier = Modifier.testTag("invite_friend_community_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite Friend", tint = PrimeBlue)
                    }
                    if (community.ownerId == currentUserId || isOfficialOwner) {
                        IconButton(onClick = onDeleteCommunity) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Community", tint = PrimeRed)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimeCardBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channelOptions) { (chId, chName) ->
                    val isSelected = selectedChannelId == chId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimeBlue else PrimeNavySurface)
                            .clickable { selectedChannelId = chId }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "#$chName",
                            color = if (isSelected) Color.White else PrimeTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = false
            ) {
                when (val msgState = messagesState) {
                    is UiDataState.Loading -> {
                        item {
                            PrimeLoadingBox(message = "Loading channel messages...")
                        }
                    }
                    is UiDataState.Error -> {
                        item {
                            PrimeErrorRetryCard(
                                message = msgState.message,
                                onRetry = { messagesRetryTrigger++ }
                            )
                        }
                    }
                    is UiDataState.Success -> {
                        val messages = msgState.data
                        if (messages.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = PrimeTextMuted, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "Welcome to #$selectedChannelName!",
                                        color = PrimeTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = "This is the start of the #$selectedChannelName channel in ${community.name}.",
                                        color = PrimeTextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(messages, key = { it.id }) { msg ->
                                val authorProfile = usersMap[msg.authorId]
                                val displayName = authorProfile?.username?.ifBlank { null } ?: msg.authorUsername
                                val avatarUrl = authorProfile?.avatarUrl?.ifBlank { null } ?: msg.authorAvatarUrl

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    GamerAvatar(
                                        avatarUrl = avatarUrl,
                                        displayName = displayName,
                                        size = 34,
                                        onClick = { if (msg.authorId.isNotBlank()) onOpenProfile(msg.authorId) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                if (msg.authorId.isNotBlank()) onOpenProfile(msg.authorId)
                                            }
                                        ) {
                                            Text(
                                                text = displayName,
                                                color = PrimeBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            VerificationBadges(
                                                isOwner = authorProfile?.isOwner == true,
                                                isVerified = authorProfile?.isVerified == true
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = formatRelativeTime(msg.createdAt),
                                                color = PrimeTextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(msg.text, color = PrimeTextPrimary, fontSize = 14.sp)
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
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Message #$selectedChannelName...") },
                    modifier = Modifier.weight(1f).testTag("community_message_input"),
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
                        if (messageText.isNotBlank()) {
                            isSending = true
                            coroutineScope.launch {
                                val res = esportRepository.sendCommunityMessage(
                                    communityId = community.id,
                                    channelId = selectedChannelId,
                                    text = messageText
                                )
                                isSending = false
                                if (res.isSuccess) {
                                    messageText = ""
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
                    enabled = messageText.isNotBlank() && !isSending,
                    modifier = Modifier.testTag("send_community_message_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PrimeBlue)
                }
            }
        }
    }

    if (showInviteSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var invitedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        val friendIds = friendships.mapNotNull { f ->
            if (f.user1 == currentUserId) f.user2 else if (f.user2 == currentUserId) f.user1 else null
        }

        ModalBottomSheet(
            onDismissRequest = { showInviteSheet = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Invite Friend to ${community.name}",
                    color = PrimeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (friendIds.isEmpty()) {
                    Text(
                        text = "Add friends first in Messages to invite them to communities.",
                        color = PrimeTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(friendIds, key = { it }) { friendUid ->
                            val friendProfile = usersMap[friendUid]
                            val name = friendProfile?.displayName?.ifBlank { friendProfile.username } ?: "Player"
                            val handle = friendProfile?.username ?: ""
                            val alreadyInvited = invitedIds.contains(friendUid)
                            val alreadyMember = members.any { it.userId == friendUid }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GamerAvatar(
                                    avatarUrl = friendProfile?.avatarUrl ?: "",
                                    displayName = name,
                                    size = 38
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (handle.isNotBlank()) {
                                        Text("@$handle", color = PrimeTextMuted, fontSize = 11.sp)
                                    }
                                }
                                if (alreadyMember) {
                                    Text("Member", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else if (alreadyInvited) {
                                    OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                        Text("Invited", color = PrimeBlue, fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val res = esportRepository.inviteUserToCommunity(
                                                    communityId = community.id,
                                                    communityName = community.name,
                                                    targetUserId = friendUid
                                                )
                                                if (res.isSuccess) {
                                                    invitedIds = invitedIds + friendUid
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        res.exceptionOrNull()?.localizedMessage ?: "Failed to send invite.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
