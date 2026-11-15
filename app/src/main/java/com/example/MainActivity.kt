package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.preferences.DataStoreManager
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreatorRepository
import com.example.data.repository.EsportRepository
import com.example.data.repository.MinecraftRepository
import com.example.data.repository.SafetyRepository
import com.example.data.repository.SocialRepository
import com.example.ui.components.NavTab
import com.example.ui.components.PrimeBottomNavigation
import com.example.ui.components.PrimeTopAppBar
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.PrimeSplashScreen
import com.example.ui.screens.auth.UsernameSetupScreen
import com.example.ui.screens.communities.CommunitiesScreen
import com.example.ui.screens.creator.CreatorStudioScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.messages.MessagesScreen
import com.example.ui.screens.minecraft.MinecraftHubScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reels.ReelsScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tournaments.TournamentsScreen
import com.example.ui.screens.upload.CreateContentSheet
import com.example.ui.screens.upload.UploadType
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeEsportTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data class Tab(val tab: NavTab) : AppScreen
    data class UserProfileRoute(val userId: String) : AppScreen
    data class Messages(
        val initialConversationId: String? = null,
        val initialOtherUserId: String? = null,
        val initialChatTitle: String? = null
    ) : AppScreen
    object MinecraftHub : AppScreen
    object CreatorStudio : AppScreen
    object Settings : AppScreen
    object Notifications : AppScreen
    object Search : AppScreen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            PrimeEsportTheme {
                PrimeEsportApp()
            }
        }
    }
}

@Composable
fun PrimeEsportApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    var hasAuthSettled by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            currentUser = fbAuth.currentUser
            hasAuthSettled = true
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val authRepository = remember(context) { AuthRepository(context) }
    val socialRepository = remember(context) { SocialRepository(context) }
    val esportRepository = remember(context) { EsportRepository(context) }
    val creatorRepository = remember(context) { CreatorRepository(context) }
    val minecraftRepository = remember(context) { MinecraftRepository(context) }
    val safetyRepository = remember(context) { SafetyRepository(context) }
    val dataStoreManager = remember(context) { DataStoreManager(context) }

    val appLanguage by dataStoreManager.appLanguage.collectAsState(initial = "en")
    val isHindi = appLanguage == "hi"

    var hasUsername by remember { mutableStateOf<Boolean?>(null) }
    var startupError by remember { mutableStateOf<String?>(null) }
    var startupRetryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUser?.uid, startupRetryTrigger) {
        val uid = currentUser?.uid
        if (uid != null) {
            hasUsername = null
            startupError = null
            val res = authRepository.refreshProfileOnStartup(uid)
            if (res.isSuccess) {
                hasUsername = res.getOrDefault(false)
            } else {
                startupError = res.exceptionOrNull()?.localizedMessage
                    ?: "Failed to verify your PRIME ESPORT profile. Please retry."
            }
        } else {
            hasUsername = null
            startupError = null
        }
    }

    if (!hasAuthSettled) {
        PrimeSplashScreen(
            statusText = if (isHindi) "PRIME ESPORT लोड हो रहा है..." else "Starting PRIME ESPORT..."
        )
    } else if (currentUser == null) {
        AuthScreen(
            authRepository = authRepository,
            onAuthSuccess = {
                currentUser = auth.currentUser
                hasAuthSettled = true
            }
        )
    } else if (hasUsername == null) {
        PrimeSplashScreen(
            statusText = if (isHindi) "आपकी PRIME ESPORT प्रोफाइल लोड हो रही है..." else "Loading your PRIME ESPORT profile...",
            errorMessage = startupError,
            onRetry = { startupRetryTrigger++ },
            onSignOut = {
                coroutineScope.launch {
                    authRepository.signOut(context, credentialManager)
                    currentUser = null
                    hasUsername = null
                    startupError = null
                }
            }
        )
    } else if (hasUsername == false) {
        UsernameSetupScreen(
            authRepository = authRepository,
            onSetupSuccess = {
                hasUsername = true
            }
        )
    } else {
        MainPlatform(
            authRepository = authRepository,
            socialRepository = socialRepository,
            esportRepository = esportRepository,
            creatorRepository = creatorRepository,
            minecraftRepository = minecraftRepository,
            safetyRepository = safetyRepository,
            dataStoreManager = dataStoreManager,
            currentUserId = currentUser!!.uid,
            isHindi = isHindi,
            onLogOut = {
                auth.signOut()
                currentUser = null
                hasUsername = null
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainPlatform(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    esportRepository: EsportRepository,
    creatorRepository: CreatorRepository,
    minecraftRepository: MinecraftRepository,
    safetyRepository: SafetyRepository,
    dataStoreManager: DataStoreManager,
    currentUserId: String,
    isHindi: Boolean,
    onLogOut: () -> Unit
) {
    val screenHistory = remember { mutableStateListOf<AppScreen>(AppScreen.Tab(NavTab.HOME)) }
    val currentScreen = screenHistory.lastOrNull() ?: AppScreen.Tab(NavTab.HOME)
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var uploadSheetMode by remember { mutableStateOf<UploadType?>(null) }

    fun selectTab(tab: NavTab) {
        currentTab = tab
        screenHistory.clear()
        screenHistory.add(AppScreen.Tab(tab))
    }

    fun navigateTo(screen: AppScreen) {
        if (screen is AppScreen.Tab) {
            selectTab(screen.tab)
        } else if (screenHistory.lastOrNull() != screen) {
            screenHistory.add(screen)
        }
    }

    fun popBack() {
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            val top = screenHistory.lastOrNull()
            if (top is AppScreen.Tab) {
                currentTab = top.tab
            }
        } else if (currentTab != NavTab.HOME) {
            selectTab(NavTab.HOME)
        }
    }

    val openProfile: (String) -> Unit = { targetUid ->
        if (targetUid == currentUserId) {
            selectTab(NavTab.PROFILE)
        } else if (targetUid.isNotBlank()) {
            navigateTo(AppScreen.UserProfileRoute(targetUid))
        }
    }

    val receivedRequests by socialRepository.observeReceivedFriendRequests().collectAsState(initial = emptyList())
    val notifications by safetyRepository.observeNotifications().collectAsState(initial = emptyList())
    val unreadMessagesCount = receivedRequests.size
    val unreadNotificationsCount = notifications.count { !it.isRead }
    val isOfficialOwner = remember(currentUserId) { authRepository.isCurrentUserOfficialOwner() }

    LaunchedEffect(currentUserId) {
        esportRepository.purgeFakeAndDuplicateEsportRecords()
    }

    BackHandler(enabled = screenHistory.size > 1 || currentTab != NavTab.HOME) {
        popBack()
    }

    val isMainTabScreen = currentScreen is AppScreen.Tab

    Scaffold(
        containerColor = PrimeDarkBackground,
        modifier = Modifier.fillMaxSize().testTag("main_platform_scaffold"),
        topBar = {
            if (isMainTabScreen) {
                PrimeTopAppBar(
                    title = "PRIME ESPORT",
                    isHindi = isHindi,
                    unreadMessagesCount = unreadMessagesCount,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onSearchClick = { navigateTo(AppScreen.Search) },
                    onUploadClick = { uploadSheetMode = UploadType.MENU },
                    onMessagesClick = { navigateTo(AppScreen.Messages()) },
                    onNotificationsClick = { navigateTo(AppScreen.Notifications) },
                    onMinecraftHubClick = { navigateTo(AppScreen.MinecraftHub) },
                    onCreatorStudioClick = { navigateTo(AppScreen.CreatorStudio) }
                )
            }
        },
        bottomBar = {
            if (isMainTabScreen) {
                PrimeBottomNavigation(
                    selectedTab = currentTab,
                    onTabSelected = { tab -> selectTab(tab) },
                    isHindi = isHindi
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMainTabScreen) innerPadding else PaddingValues()),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                when (val screen = currentScreen) {
                    is AppScreen.Tab -> {
                        when (screen.tab) {
                            NavTab.HOME -> {
                                HomeScreen(
                                    authRepository = authRepository,
                                    socialRepository = socialRepository,
                                    safetyRepository = safetyRepository,
                                    creatorRepository = creatorRepository,
                                    currentUserId = currentUserId,
                                    isHindi = isHindi,
                                    onOpenProfile = openProfile,
                                    onOpenUploadSheet = { mode -> uploadSheetMode = mode }
                                )
                            }
                            NavTab.REELS -> {
                                ReelsScreen(
                                    authRepository = authRepository,
                                    socialRepository = socialRepository,
                                    safetyRepository = safetyRepository,
                                    dataStoreManager = dataStoreManager,
                                    currentUserId = currentUserId,
                                    onOpenProfile = openProfile,
                                    onOpenUploadSheet = { mode -> uploadSheetMode = mode }
                                )
                            }
                            NavTab.COMMUNITIES -> {
                                CommunitiesScreen(
                                    authRepository = authRepository,
                                    socialRepository = socialRepository,
                                    esportRepository = esportRepository,
                                    currentUserId = currentUserId,
                                    isOfficialOwner = isOfficialOwner,
                                    isHindi = isHindi,
                                    onOpenProfile = openProfile
                                )
                            }
                            NavTab.TOURNAMENTS -> {
                                TournamentsScreen(
                                    authRepository = authRepository,
                                    esportRepository = esportRepository,
                                    currentUserId = currentUserId,
                                    isOfficialOwner = isOfficialOwner,
                                    isHindi = isHindi,
                                    onOpenProfile = openProfile
                                )
                            }
                            NavTab.PROFILE -> {
                                androidx.compose.runtime.key(currentUserId) {
                                    ProfileScreen(
                                        authRepository = authRepository,
                                        socialRepository = socialRepository,
                                        creatorRepository = creatorRepository,
                                        safetyRepository = safetyRepository,
                                        userId = currentUserId,
                                        currentUserId = currentUserId,
                                        isHindi = isHindi,
                                        showBackButton = false,
                                        onBack = { popBack() },
                                        onOpenProfile = openProfile,
                                        onOpenMessages = { navigateTo(AppScreen.Messages()) },
                                        onOpenChat = { convId, otherUid, title ->
                                            navigateTo(
                                                AppScreen.Messages(
                                                    initialConversationId = convId,
                                                    initialOtherUserId = otherUid,
                                                    initialChatTitle = title
                                                )
                                            )
                                        },
                                        onOpenUploadSheet = { mode -> uploadSheetMode = mode },
                                        onNavigateToCreatorStudio = { navigateTo(AppScreen.CreatorStudio) },
                                        onNavigateToSettings = { navigateTo(AppScreen.Settings) }
                                    )
                                }
                            }
                        }
                    }
                    is AppScreen.UserProfileRoute -> {
                        androidx.compose.runtime.key(screen.userId) {
                            ProfileScreen(
                                authRepository = authRepository,
                                socialRepository = socialRepository,
                                creatorRepository = creatorRepository,
                                safetyRepository = safetyRepository,
                                userId = screen.userId,
                                currentUserId = currentUserId,
                                isHindi = isHindi,
                                showBackButton = true,
                                onBack = { popBack() },
                                onOpenProfile = openProfile,
                                onOpenMessages = { navigateTo(AppScreen.Messages()) },
                                onOpenChat = { convId, otherUid, title ->
                                    navigateTo(
                                        AppScreen.Messages(
                                            initialConversationId = convId,
                                            initialOtherUserId = otherUid,
                                            initialChatTitle = title
                                        )
                                    )
                                },
                                onOpenUploadSheet = { mode -> uploadSheetMode = mode },
                                onNavigateToCreatorStudio = { navigateTo(AppScreen.CreatorStudio) },
                                onNavigateToSettings = { navigateTo(AppScreen.Settings) }
                            )
                        }
                    }
                    is AppScreen.Messages -> {
                        MessagesScreen(
                            authRepository = authRepository,
                            socialRepository = socialRepository,
                            safetyRepository = safetyRepository,
                            currentUserId = currentUserId,
                            initialConversationId = screen.initialConversationId,
                            initialOtherUserId = screen.initialOtherUserId,
                            initialChatTitle = screen.initialChatTitle,
                            onBack = { popBack() },
                            onOpenProfile = openProfile
                        )
                    }
                    is AppScreen.MinecraftHub -> {
                        MinecraftHubScreen(
                            authRepository = authRepository,
                            minecraftRepository = minecraftRepository,
                            currentUserId = currentUserId,
                            onBack = { popBack() },
                            onOpenProfile = openProfile
                        )
                    }
                    is AppScreen.CreatorStudio -> {
                        CreatorStudioScreen(
                            authRepository = authRepository,
                            socialRepository = socialRepository,
                            creatorRepository = creatorRepository,
                            currentUserId = currentUserId,
                            onBack = { popBack() },
                            onOpenProfile = openProfile
                        )
                    }
                    is AppScreen.Settings -> {
                        SettingsScreen(
                            authRepository = authRepository,
                            dataStoreManager = dataStoreManager,
                            onBack = { popBack() },
                            onLoggedOut = onLogOut
                        )
                    }
                    is AppScreen.Notifications -> {
                        NotificationsScreen(
                            authRepository = authRepository,
                            safetyRepository = safetyRepository,
                            currentUserId = currentUserId,
                            onBack = { popBack() },
                            onNavigateToProfile = openProfile,
                            onNavigateToTournament = { selectTab(NavTab.TOURNAMENTS) },
                            onNavigateToMessages = { navigateTo(AppScreen.Messages()) },
                            onNavigateToCommunity = { selectTab(NavTab.COMMUNITIES) },
                            isHindi = isHindi
                        )
                    }
                    is AppScreen.Search -> {
                        SearchScreen(
                            authRepository = authRepository,
                            socialRepository = socialRepository,
                            creatorRepository = creatorRepository,
                            esportRepository = esportRepository,
                            minecraftRepository = minecraftRepository,
                            safetyRepository = safetyRepository,
                            currentUserId = currentUserId,
                            onBack = { popBack() },
                            onSelectPlayer = openProfile,
                            onSelectCommunity = { selectTab(NavTab.COMMUNITIES) },
                            onSelectTournament = { selectTab(NavTab.TOURNAMENTS) },
                            onSelectMinecraft = { navigateTo(AppScreen.MinecraftHub) },
                            isHindi = isHindi
                        )
                    }
                }
            }
        }
    }

    uploadSheetMode?.let { mode ->
        CreateContentSheet(
            socialRepository = socialRepository,
            creatorRepository = creatorRepository,
            currentUserId = currentUserId,
            initialMode = mode,
            onDismiss = { uploadSheetMode = null }
        )
    }
}
