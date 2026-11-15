package com.example.ui.screens.tournaments

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Tournament
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.EsportRepository
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.PrimeErrorRetryCard
import com.example.ui.components.PrimeLoadingBox
import com.example.ui.components.VerificationBadges
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
fun TournamentsScreen(
    authRepository: AuthRepository,
    esportRepository: EsportRepository,
    currentUserId: String,
    isOfficialOwner: Boolean = false,
    isHindi: Boolean = false,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val tournamentsState by remember(retryTrigger) {
        esportRepository.observeTournamentsState()
    }.collectAsState(initial = UiDataState.Loading)
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())
    val currentUserProfile by authRepository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val canHostTournaments = isOfficialOwner || currentUserProfile?.isOwner == true || currentUserProfile?.isVerified == true

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        if (isHindi) "आगामी" else "Upcoming",
        if (isHindi) "लाइव" else "Live",
        if (isHindi) "पूर्ण" else "Completed"
    )
    val statusKeys = listOf("upcoming", "ongoing", "completed")

    var selectedTournamentForRegistration by remember { mutableStateOf<Tournament?>(null) }
    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var tournamentPendingDelete by remember { mutableStateOf<Tournament?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = PrimeDarkBackground,
        floatingActionButton = {
            if (canHostTournaments) {
                FloatingActionButton(
                    onClick = { showCreateTournamentDialog = true },
                    containerColor = PrimeBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("create_tournament_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Tournament")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (isHindi) "प्राइम एस्पोर्ट्स टूर्नामेंट" else "PRIME TOURNAMENTS",
                        color = PrimeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isHindi) "प्रतिस्पर्धा करें, लीडरबोर्ड पर चढ़ें और पुरस्कार जीतें" else "Compete with squads, join brackets, and host community events.",
                        color = PrimeTextSecondary,
                        fontSize = 13.sp
                    )
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
                                    color = if (selectedTab == index) PrimeBlue else PrimeTextSecondary
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentPadding = PaddingValues(bottom = 84.dp)
                ) {
                    when (val state = tournamentsState) {
                        is UiDataState.Loading -> {
                            item {
                                PrimeLoadingBox(
                                    message = if (isHindi) "टूर्नामेंट्स लोड हो रहे हैं..." else "Loading tournaments..."
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
                            val filteredTournaments = state.data.filter { it.status == statusKeys[selectedTab] }
                            if (filteredTournaments.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp, horizontal = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("🏆", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No tournaments available right now.",
                                            color = PrimeTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (canHostTournaments) {
                                                "Organize a verified tournament for the PRIME ESPORT community."
                                            } else {
                                                "Verified creators and official organizers can publish tournaments here."
                                            },
                                            color = PrimeTextSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                        )
                                        if (canHostTournaments) {
                                            Button(
                                                onClick = { showCreateTournamentDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Host Tournament", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(filteredTournaments, key = { it.id }) { tournament ->
                                    TournamentCard(
                                        tournament = tournament,
                                        organizerProfile = usersMap[tournament.organizerId],
                                        canDelete = tournament.organizerId == currentUserId || isOfficialOwner,
                                        onRegisterClick = { selectedTournamentForRegistration = tournament },
                                        onDeleteClick = {
                                            deleteError = null
                                            tournamentPendingDelete = tournament
                                        },
                                        onOrganizerClick = {
                                            if (tournament.organizerId.isNotBlank()) {
                                                onOpenProfile(tournament.organizerId)
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

    // Register Team Sheet
    selectedTournamentForRegistration?.let { tournament ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedTournamentForRegistration = null },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            var teamName by remember { mutableStateOf("") }
            var playerUsernames by remember { mutableStateOf("") }
            var isRegistering by remember { mutableStateOf(false) }
            var registrationSuccess by remember { mutableStateOf(false) }
            var registrationError by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Tournament Registration",
                    color = PrimeTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tournament.title} • ${tournament.game} (${tournament.teamSize} Players/Team)",
                    color = PrimeBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                if (registrationSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimeNavySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Registration Confirmed!",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Your team '$teamName' is registered for ${tournament.title}.",
                                color = PrimeTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = {
                            teamName = it
                            registrationError = null
                        },
                        label = { Text("Team Name / Clan Tag") },
                        placeholder = { Text("Enter your team or clan name") },
                        modifier = Modifier.fillMaxWidth().testTag("team_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimeTextPrimary,
                            unfocusedTextColor = PrimeTextPrimary,
                            focusedBorderColor = PrimeBlue,
                            unfocusedBorderColor = PrimeBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = playerUsernames,
                        onValueChange = {
                            playerUsernames = it
                            registrationError = null
                        },
                        label = { Text("Player Usernames / IGNs") },
                        placeholder = { Text("Enter player usernames separated by commas") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("player_igns_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimeTextPrimary,
                            unfocusedTextColor = PrimeTextPrimary,
                            focusedBorderColor = PrimePurple,
                            unfocusedBorderColor = PrimeBorder
                        )
                    )

                    if (registrationError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(registrationError ?: "", color = PrimeRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (teamName.isNotBlank() && playerUsernames.isNotBlank()) {
                                isRegistering = true
                                registrationError = null
                                coroutineScope.launch {
                                    val res = esportRepository.registerForTournament(
                                        tournamentId = tournament.id,
                                        teamName = teamName,
                                        playerUsernames = playerUsernames
                                    )
                                    isRegistering = false
                                    if (res.isSuccess) {
                                        registrationSuccess = true
                                    } else {
                                        registrationError = res.exceptionOrNull()?.localizedMessage
                                            ?: "Failed to register team."
                                    }
                                }
                            }
                        },
                        enabled = teamName.isNotBlank() && playerUsernames.isNotBlank() && !isRegistering,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_registration_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Confirm Registration", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Host Tournament Dialog
    if (showCreateTournamentDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCreateTournamentDialog = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            var title by remember { mutableStateOf("") }
            var game by remember { mutableStateOf("BGMI") }
            var prizePool by remember { mutableStateOf("") }
            var startDate by remember { mutableStateOf("") }
            var teamSize by remember { mutableIntStateOf(4) }
            var description by remember { mutableStateOf("") }
            var isHosting by remember { mutableStateOf(false) }
            var createError by remember { mutableStateOf<String?>(null) }

            val games = listOf("BGMI", "Minecraft", "Free Fire", "Valorant", "COD Mobile")
            val teamSizes = listOf(1 to "Solo", 2 to "Duo", 4 to "Squad", 5 to "5v5")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Host Esports Tournament",
                    color = PrimeTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        createError = null
                    },
                    label = { Text("Tournament Title") },
                    placeholder = { Text("Enter tournament name") },
                    modifier = Modifier.fillMaxWidth().testTag("tournament_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Game:", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(games) { g ->
                        FilterChip(
                            selected = game == g,
                            onClick = { game = g },
                            label = { Text(g, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimeBlue,
                                selectedLabelColor = Color.White,
                                containerColor = PrimeNavySurface,
                                labelColor = PrimeTextSecondary
                            )
                        )
                    }
                }

                Text("Team Size:", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teamSizes) { (sizeVal, sizeLabel) ->
                        FilterChip(
                            selected = teamSize == sizeVal,
                            onClick = { teamSize = sizeVal },
                            label = { Text("$sizeLabel ($sizeVal)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimePurple,
                                selectedLabelColor = Color.White,
                                containerColor = PrimeNavySurface,
                                labelColor = PrimeTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = prizePool,
                    onValueChange = { prizePool = it },
                    label = { Text("Prize Pool / Reward") },
                    placeholder = { Text("e.g. Custom Room, Bragging Rights, or Pool") },
                    modifier = Modifier.fillMaxWidth().testTag("tournament_prize_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeRed,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date & Time") },
                    placeholder = { Text("e.g. Saturday 8:00 PM IST") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimePurple,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Rules (Optional)") },
                    placeholder = { Text("Match format, map pool, fair play rules...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                if (createError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(createError ?: "", color = PrimeRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isHosting || title.isBlank()) return@Button
                        isHosting = true
                        createError = null
                        coroutineScope.launch {
                            val res = esportRepository.createTournament(
                                title = title,
                                game = game,
                                description = description.trim(),
                                prizePool = prizePool.trim(),
                                teamSize = teamSize,
                                maxTeams = 32,
                                startDate = startDate.trim(),
                                rules = description.trim()
                            )
                            isHosting = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Tournament published!", Toast.LENGTH_SHORT).show()
                                showCreateTournamentDialog = false
                            } else {
                                createError = res.exceptionOrNull()?.localizedMessage ?: "Failed to host tournament."
                            }
                        }
                    },
                    enabled = canHostTournaments && title.isNotBlank() && !isHosting,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_tournament_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isHosting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Publish Tournament", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    tournamentPendingDelete?.let { tournament ->
        DeleteConfirmationDialog(
            title = "Delete Tournament",
            message = "Remove '${tournament.title}' from PRIME TOURNAMENTS? This action cannot be undone.",
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onConfirm = {
                isDeleting = true
                deleteError = null
                coroutineScope.launch {
                    val res = esportRepository.deleteTournament(tournament.id)
                    isDeleting = false
                    if (res.isSuccess) {
                        tournamentPendingDelete = null
                        Toast.makeText(context, "Tournament deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        deleteError = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete tournament."
                    }
                }
            },
            onDismiss = { tournamentPendingDelete = null }
        )
    }
}

@Composable
fun TournamentCard(
    tournament: Tournament,
    organizerProfile: UserProfile?,
    canDelete: Boolean,
    onRegisterClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOrganizerClick: () -> Unit
) {
    val organizerName = organizerProfile?.displayName?.ifBlank { organizerProfile.username }
        ?: tournament.organizerName.ifBlank { "Creator" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimeBlue.copy(alpha = 0.2f))
                        .border(1.dp, PrimeBlue, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tournament.game.uppercase(),
                        color = PrimeBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (tournament.status) {
                                    "ongoing" -> PrimeRed.copy(alpha = 0.2f)
                                    "completed" -> PrimeTextMuted.copy(alpha = 0.2f)
                                    else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (tournament.status) {
                                "ongoing" -> "LIVE"
                                "completed" -> "COMPLETED"
                                else -> "REGISTRATIONS OPEN"
                            },
                            color = when (tournament.status) {
                                "ongoing" -> PrimeRed
                                "completed" -> PrimeTextMuted
                                else -> Color(0xFF10B981)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(34.dp).testTag("delete_tournament_${tournament.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Tournament",
                                tint = PrimeRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tournament.title,
                color = PrimeTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onOrganizerClick() }
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Organized by $organizerName",
                    color = PrimeTextSecondary,
                    fontSize = 12.sp
                )
                VerificationBadges(
                    isOwner = organizerProfile?.isOwner == true,
                    isVerified = organizerProfile?.isVerified == true
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimeNavySurface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRIZE / REWARD", color = PrimeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = tournament.prizePool.ifBlank { "Not specified" },
                        color = if (tournament.prizePool.isBlank()) PrimeTextSecondary else PrimeRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("TEAM SIZE", color = PrimeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${tournament.teamSize} Players", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TEAMS", color = PrimeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${tournament.registeredTeamsCount}/${tournament.maxTeams}", color = PrimePurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimeTextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tournament.startDate.ifBlank { "Schedule TBA" },
                        color = PrimeTextMuted,
                        fontSize = 12.sp
                    )
                }

                if (tournament.status == "upcoming") {
                    Button(
                        onClick = onRegisterClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Register", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
