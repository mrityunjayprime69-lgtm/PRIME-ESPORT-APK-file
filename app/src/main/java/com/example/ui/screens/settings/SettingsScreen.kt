package com.example.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.preferences.DataSaverSettings
import com.example.data.preferences.DataStoreManager
import com.example.data.repository.AuthRepository
import com.example.ui.components.DeleteConfirmationDialog
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
fun SettingsScreen(
    authRepository: AuthRepository,
    dataStoreManager: DataStoreManager,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val dataSaverSettings by dataStoreManager.dataSaverSettings.collectAsState(initial = DataSaverSettings())
    val appLanguage by dataStoreManager.appLanguage.collectAsState(initial = "en")

    var showDataSaverSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

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
                    Text(
                        text = if (appLanguage == "hi") "सेटिंग्स" else "SETTINGS",
                        color = PrimeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Account Section
            item {
                SettingsSectionHeader("ACCOUNT & SECURITY")
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(authRepository.currentUser?.displayName?.ifBlank { "Player" } ?: "Player", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(authRepository.currentUser?.email ?: "Signed in account", color = PrimeTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Preferences Section: Data Saver & Language
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader("PREFERENCES & DATA")

                // Data Saver Item
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showDataSaverSheet = true },
                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DataSaverOn, contentDescription = null, tint = PrimeRed, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Data Saver", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (dataSaverSettings.masterDataSaver) "Enabled • Low data mode active" else "Video autoplay, preloading & quality controls",
                                    color = if (dataSaverSettings.masterDataSaver) PrimeBlue else PrimeTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = dataSaverSettings.masterDataSaver,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { dataStoreManager.setMasterDataSaver(checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimeRed
                            ),
                            modifier = Modifier.testTag("master_data_saver_switch")
                        )
                    }
                }

                // Language Item
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showLanguageSheet = true },
                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = PrimePurple, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Language / भाषा", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (appLanguage == "hi") "हिन्दी (Hindi)" else "English", color = PrimeBlue, fontSize = 12.sp)
                            }
                        }
                        Text("Change", color = PrimeBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Clear Cache Item
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        val freedBytes = dataStoreManager.clearCache()
                        val freedKb = freedBytes / 1024
                        cacheMessage = "Cleared $freedKb KB cache successfully"
                        Toast.makeText(context, cacheMessage, Toast.LENGTH_SHORT).show()
                    },
                    colors = CardDefaults.cardColors(containerColor = PrimeCardBackground),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear App Cache", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(cacheMessage ?: "Free storage and temporary image cache", color = PrimeTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Actions: Logout & Delete Account
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSectionHeader("SESSION")

                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.signOut(context, credentialManager)
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeNavySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PrimeBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from PRIME ESPORT", color = PrimeBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        deleteAccountError = null
                        showDeleteAccountDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_account_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = PrimeRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = PrimeRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Data Saver Bottom Sheet (Section 19 Requirements)
    if (showDataSaverSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDataSaverSheet = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Data Saver Settings", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Reduce mobile network consumption and optimize video playback.",
                    color = PrimeTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // Granular switches
                DataSaverToggleRow(
                    title = "Auto-play Feed Videos",
                    checked = dataSaverSettings.autoPlayVideos,
                    enabled = !dataSaverSettings.masterDataSaver,
                    onCheckedChange = { coroutineScope.launch { dataStoreManager.setAutoPlayVideos(it) } }
                )
                DataSaverToggleRow(
                    title = "Auto-play Reels",
                    checked = dataSaverSettings.autoPlayReels,
                    enabled = !dataSaverSettings.masterDataSaver,
                    onCheckedChange = { coroutineScope.launch { dataStoreManager.setAutoPlayReels(it) } }
                )
                DataSaverToggleRow(
                    title = "Preload Media",
                    checked = dataSaverSettings.preloadVideos,
                    enabled = !dataSaverSettings.masterDataSaver,
                    onCheckedChange = { coroutineScope.launch { dataStoreManager.setPreloadVideos(it) } }
                )
                DataSaverToggleRow(
                    title = "Download Only on Wi-Fi",
                    checked = dataSaverSettings.downloadWifiOnly,
                    enabled = !dataSaverSettings.masterDataSaver,
                    onCheckedChange = { coroutineScope.launch { dataStoreManager.setDownloadWifiOnly(it) } }
                )
                DataSaverToggleRow(
                    title = "Upload Only on Wi-Fi",
                    checked = dataSaverSettings.uploadWifiOnly,
                    enabled = !dataSaverSettings.masterDataSaver,
                    onCheckedChange = { coroutineScope.launch { dataStoreManager.setUploadWifiOnly(it) } }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showDataSaverSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Language Selector Sheet
    if (showLanguageSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = sheetState,
            containerColor = PrimeCardBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Select Language / भाषा चुनें", color = PrimeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (appLanguage == "en") PrimeNavySurface else Color.Transparent)
                        .border(1.dp, if (appLanguage == "en") PrimeBlue else PrimeBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch {
                                dataStoreManager.setLanguage("en")
                                showLanguageSheet = false
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("English", color = PrimeTextPrimary, fontWeight = FontWeight.Bold)
                    if (appLanguage == "en") Text("✓", color = PrimeBlue, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (appLanguage == "hi") PrimeNavySurface else Color.Transparent)
                        .border(1.dp, if (appLanguage == "hi") PrimeBlue else PrimeBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch {
                                dataStoreManager.setLanguage("hi")
                                showLanguageSheet = false
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("हिन्दी (Hindi)", color = PrimeTextPrimary, fontWeight = FontWeight.Bold)
                    if (appLanguage == "hi") Text("✓", color = PrimeBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteAccountDialog) {
        DeleteConfirmationDialog(
            title = "Delete Account",
            message = "Permanently delete your PRIME ESPORT profile and release your username? This cannot be undone.",
            isDeleting = isDeletingAccount,
            errorMessage = deleteAccountError,
            onConfirm = {
                isDeletingAccount = true
                deleteAccountError = null
                coroutineScope.launch {
                    val res = authRepository.deleteAccount()
                    isDeletingAccount = false
                    if (res.isSuccess) {
                        showDeleteAccountDialog = false
                        onLoggedOut()
                    } else {
                        deleteAccountError = res.exceptionOrNull()?.localizedMessage
                            ?: "Please sign in again recently before deleting your account."
                    }
                }
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = PrimeTextMuted,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}

@Composable
fun DataSaverToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = if (enabled) PrimeTextPrimary else PrimeTextMuted, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimeBlue
            )
        )
    }
}
