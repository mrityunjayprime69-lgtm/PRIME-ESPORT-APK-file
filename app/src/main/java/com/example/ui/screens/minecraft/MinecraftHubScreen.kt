package com.example.ui.screens.minecraft

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MinecraftServer
import com.example.data.model.UiDataState
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.MinecraftRepository
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
fun MinecraftHubScreen(
    authRepository: AuthRepository,
    minecraftRepository: MinecraftRepository,
    currentUserId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var retryTrigger by remember { mutableIntStateOf(0) }
    val serversState by remember(retryTrigger) {
        minecraftRepository.observeMinecraftServersState()
    }.collectAsState(initial = UiDataState.Loading)
    val servers = (serversState as? UiDataState.Success)?.data ?: emptyList()
    val usersMap by authRepository.observeUsersMap().collectAsState(initial = emptyMap())

    var showAddServerDialog by remember { mutableStateOf(false) }
    var serverPendingDelete by remember { mutableStateOf<MinecraftServer?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

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
                    Column {
                        Text("PRIME MINECRAFT HUB", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Java • Bedrock • Crossplay Directory", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddServerDialog = true },
                containerColor = Color(0xFF10B981),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_minecraft_server_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Server")
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimeNavySurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBlue.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Minecraft Server Directory", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "Browse community-submitted servers or list your own server IP and port.",
                                    color = PrimeTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                when (val currentState = serversState) {
                    is UiDataState.Loading -> {
                        item {
                            PrimeLoadingBox(
                                message = "Loading Minecraft servers...",
                                accentColor = Color(0xFF10B981)
                            )
                        }
                    }
                    is UiDataState.Error -> {
                        item {
                            PrimeErrorRetryCard(
                                message = currentState.message,
                                accentColor = Color(0xFF10B981),
                                onRetry = { retryTrigger++ }
                            )
                        }
                    }
                    is UiDataState.Success -> if (servers.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⛏️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Minecraft servers available yet.",
                                    color = PrimeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Be the first to list your SMP, minigame network, or clan server for the community!",
                                    color = PrimeTextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                                )
                                Button(
                                    onClick = { showAddServerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Server", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        items(servers, key = { it.id }) { server ->
                            MinecraftServerCard(
                                server = server,
                                ownerProfile = usersMap[server.ownerId],
                                isOwner = server.ownerId == currentUserId,
                                onCopyAddress = {
                                    val fullAddr = if (server.port == 25565) server.address else "${server.address}:${server.port}"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Server IP", fullAddr)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied $fullAddr to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteClick = {
                                    deleteError = null
                                    serverPendingDelete = server
                                },
                                onOwnerClick = {
                                    if (server.ownerId.isNotBlank()) {
                                        onOpenProfile(server.ownerId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddServerDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddServerDialog = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            var name by remember { mutableStateOf("") }
            var edition by remember { mutableStateOf("Crossplay") }
            var version by remember { mutableStateOf("") }
            var address by remember { mutableStateOf("") }
            var port by remember { mutableStateOf("25565") }
            var description by remember { mutableStateOf("") }
            var isAdding by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val editions = listOf("Crossplay", "Java", "Bedrock")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("List Minecraft Server", color = PrimeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Server Name") },
                    placeholder = { Text("Enter server name") },
                    modifier = Modifier.fillMaxWidth().testTag("server_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Edition:", color = PrimeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(editions) { ed ->
                        FilterChip(
                            selected = edition == ed,
                            onClick = { edition = ed },
                            label = { Text(ed, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White,
                                containerColor = PrimeNavySurface,
                                labelColor = PrimeTextSecondary
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            errorMessage = null
                        },
                        label = { Text("Server Address") },
                        placeholder = { Text("play.example.net") },
                        modifier = Modifier.weight(2f).testTag("server_address_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimeTextPrimary,
                            unfocusedTextColor = PrimeTextPrimary,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = PrimeBorder
                        )
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PrimeTextPrimary,
                            unfocusedTextColor = PrimeTextPrimary,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = PrimeBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Minecraft Version") },
                    placeholder = { Text("e.g. 1.21.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Server Description") },
                    placeholder = { Text("Game modes, rules, Discord info...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimeTextPrimary,
                        unfocusedTextColor = PrimeTextPrimary,
                        focusedBorderColor = PrimeBlue,
                        unfocusedBorderColor = PrimeBorder
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(errorMessage ?: "", color = PrimeRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank() && address.isNotBlank()) {
                            isAdding = true
                            errorMessage = null
                            coroutineScope.launch {
                                val res = minecraftRepository.addMinecraftServer(
                                    name = name,
                                    edition = edition,
                                    version = version.ifBlank { "Latest" },
                                    address = address,
                                    port = port.toIntOrNull() ?: 25565,
                                    description = description
                                )
                                isAdding = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Server added to directory!", Toast.LENGTH_SHORT).show()
                                    showAddServerDialog = false
                                } else {
                                    errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to add server."
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && address.isNotBlank() && !isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_server_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Add to Server Directory", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    serverPendingDelete?.let { server ->
        DeleteConfirmationDialog(
            title = "Remove Minecraft Server",
            message = "Remove '${server.name}' from the server directory?",
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onConfirm = {
                isDeleting = true
                deleteError = null
                coroutineScope.launch {
                    val res = minecraftRepository.deleteMinecraftServer(server.id)
                    isDeleting = false
                    if (res.isSuccess) {
                        serverPendingDelete = null
                        Toast.makeText(context, "Server listing removed", Toast.LENGTH_SHORT).show()
                    } else {
                        deleteError = res.exceptionOrNull()?.localizedMessage ?: "Failed to remove server."
                    }
                }
            },
            onDismiss = { serverPendingDelete = null }
        )
    }
}

@Composable
fun MinecraftServerCard(
    server: MinecraftServer,
    ownerProfile: UserProfile?,
    isOwner: Boolean,
    onCopyAddress: () -> Unit,
    onDeleteClick: () -> Unit,
    onOwnerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
                        .background(PrimeNavySurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = server.edition.ifBlank { "Crossplay" },
                        color = PrimePurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimeBlue.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Version: ${server.version.ifBlank { "Latest" }}",
                            color = PrimeBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                    if (isOwner) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp).testTag("delete_server_${server.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Server",
                                tint = PrimeRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(server.name, color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (server.description.isNotBlank()) {
                Text(
                    text = server.description,
                    color = PrimeTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimeNavySurface)
                    .clickable { onCopyAddress() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (server.port == 25565) server.address else "${server.address}:${server.port}",
                        color = PrimeTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", tint = PrimeBlue, modifier = Modifier.size(16.dp))
            }

            if (ownerProfile != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOwnerClick() }
                ) {
                    Text(
                        text = "Listed by @${ownerProfile.username}",
                        color = PrimeTextMuted,
                        fontSize = 12.sp
                    )
                    VerificationBadges(
                        isOwner = ownerProfile.isOwner,
                        isVerified = ownerProfile.isVerified
                    )
                }
            }
        }
    }
}
