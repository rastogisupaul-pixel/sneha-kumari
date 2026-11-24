package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.E2eeCryptoManager
import com.example.core.EnglishStrings
import com.example.core.HindiStrings
import com.example.core.LocalSmileStrings
import com.example.core.SmileNotificationHelper
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.MessageType
import com.example.data.repository.ChatRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.SocialAndCallRepository
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOut
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewChatScreen
import com.example.ui.screens.ProfileQrScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.SmileTheme
import com.example.ui.theme.SmileYellow
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object SmileRoutes {
    const val HOME = "home"
    const val NEW_CHAT = "new_chat"
    const val NEW_GROUP = "new_group"
    const val SETTINGS = "settings"
    const val QR_CODE = "qr_code"
    const val CHAT = "chat/{chatId}"
    fun chat(chatId: String) = "chat/$chatId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SmileNotificationHelper.ensureChannels(this)
        setContent {
            SmileRootApp()
        }
    }
}

@Composable
fun SmileRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by remember { mutableStateOf(true) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (showSplash) {
        SmileTheme(darkTheme = false) {
            SplashScreen(onFinished = { showSplash = false })
        }
        return
    }

    val user = currentUser
    if (user == null) {
        SmileTheme(darkTheme = false) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedSmileApp(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: "",
                onSignOut = {
                    signOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedSmileApp(
    currentUserId: String,
    defaultDisplayName: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val sessionVm: SessionViewModel = viewModel(
        key = "session_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                val crypto = E2eeCryptoManager(app)
                SessionViewModel(ProfileRepository(db, crypto), currentUserId)
            }
        }
    )

    val homeVm: HomeViewModel = viewModel(
        key = "home_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                val crypto = E2eeCryptoManager(app)
                HomeViewModel(
                    ChatRepository(db, crypto),
                    SocialAndCallRepository(db),
                    currentUserId
                )
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { sessionVm.setOfflineOnDispose() }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val accountState by sessionVm.accountState.collectAsStateWithLifecycle()
    val myProfileState by sessionVm.myProfileState.collectAsStateWithLifecycle()
    val contacts by sessionVm.contacts.collectAsStateWithLifecycle()
    val directoryProfiles by sessionVm.directoryProfiles.collectAsStateWithLifecycle()
    val blockedUsers by sessionVm.blockedUsers.collectAsStateWithLifecycle()
    val isAppUnlocked by sessionVm.isAppUnlocked.collectAsStateWithLifecycle()
    val actionMessage by sessionVm.actionMessage.collectAsStateWithLifecycle()
    val isBusy by sessionVm.isBusy.collectAsStateWithLifecycle()
    val searchResult by sessionVm.searchResultProfile.collectAsStateWithLifecycle()

    val chatsState by homeVm.chatsState.collectAsStateWithLifecycle()
    val statusesState by homeVm.statusesState.collectAsStateWithLifecycle()
    val callsState by homeVm.callsState.collectAsStateWithLifecycle()
    val communitiesState by homeVm.communitiesState.collectAsStateWithLifecycle()
    val viewedStatusIds by homeVm.viewedStatusIds.collectAsStateWithLifecycle()
    val homeFeedback by homeVm.feedbackMessage.collectAsStateWithLifecycle()

    val account = (accountState as? UiState.Success)?.data
    val myProfile = (myProfileState as? UiState.Success)?.data
    val darkTheme = account?.darkMode == true
    val strings = if (account?.language == "hi") HindiStrings else EnglishStrings

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            sessionVm.clearActionMessage()
        }
    }
    LaunchedEffect(homeFeedback) {
        homeFeedback?.let {
            snackbarHostState.showSnackbar(it)
            homeVm.clearFeedback()
        }
    }

    // Active ringing/accepted call detection
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()
    val activeCall = remember(calls) {
        calls.firstOrNull {
            it.status == CallStatus.RINGING.name || it.status == CallStatus.ACCEPTED.name
        }
    }

    // Trigger local notification when an incoming call rings
    LaunchedEffect(activeCall?.id, activeCall?.status) {
        if (activeCall != null && activeCall.callerId != currentUserId && activeCall.status == CallStatus.RINGING.name) {
            SmileNotificationHelper.showNotification(
                context = context,
                channelId = SmileNotificationHelper.CHANNEL_CALLS,
                notifId = activeCall.id.hashCode(),
                title = "Incoming SMILE ${if (activeCall.callType.contains("VIDEO")) "Video" else "Voice"} Call",
                body = "${activeCall.callerName} is calling you..."
            )
        }
    }

    CompositionLocalProvider(LocalSmileStrings provides strings) {
        SmileTheme(darkTheme = darkTheme) {
            // Loading gate while profile loads
            if (myProfileState is UiState.Loading || accountState is UiState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmileYellow)
                }
                return@SmileTheme
            }

            // First-time registration / +91 phone profile setup
            if (myProfile == null) {
                ProfileSetupScreen(
                    initialName = defaultDisplayName,
                    isSaving = isBusy,
                    errorText = actionMessage,
                    onSaveProfile = { phone, name, smileId, about, photo ->
                        sessionVm.saveProfileSetup(phone, name, smileId, about, photo)
                    }
                )
                return@SmileTheme
            }

            // App Lock Screen if PIN or Biometric Lock is enabled
            val hasLock = !account?.appLockPinHash.isNullOrBlank() || account?.biometricLockEnabled == true
            if (hasLock && !isAppUnlocked) {
                AppLockScreen(
                    storedPinHash = account?.appLockPinHash ?: "",
                    biometricEnabled = account?.biometricLockEnabled == true,
                    onUnlockSuccess = { sessionVm.unlockApp() },
                    onVerifyPin = { pin ->
                        sessionVm.verifyPinAndUnlock(pin, account?.appLockPinHash ?: "")
                    }
                )
                return@SmileTheme
            }

            // Active Call Full-Screen Overlay
            if (activeCall != null) {
                val profileMap = remember(directoryProfiles) { directoryProfiles.associateBy { it.userId } }
                val peerUid = activeCall.participantUids.firstOrNull { it != currentUserId }
                val peerName = if (activeCall.callerId == currentUserId) {
                    peerUid?.let { profileMap[it]?.displayName } ?: "SMILE User"
                } else {
                    activeCall.callerName
                }
                val peerPhoto = peerUid?.let { profileMap[it]?.photoBase64 } ?: ""

                CallScreen(
                    call = activeCall,
                    currentUserId = currentUserId,
                    peerName = peerName,
                    peerPhotoBase64 = peerPhoto,
                    onAccept = { homeVm.updateCallStatus(activeCall.id, CallStatus.ACCEPTED) },
                    onReject = { homeVm.updateCallStatus(activeCall.id, CallStatus.REJECTED) },
                    onEnd = { dur ->
                        val finalStatus = if (activeCall.status == CallStatus.RINGING.name) {
                            CallStatus.MISSED
                        } else {
                            CallStatus.ENDED
                        }
                        homeVm.updateCallStatus(activeCall.id, finalStatus, dur)
                    }
                )
                return@SmileTheme
            }

            val navController = rememberNavController()
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { rootPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(rootPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = SmileRoutes.HOME
                    ) {
                        composable(SmileRoutes.HOME) {
                            HomeScreen(
                                currentUserId = currentUserId,
                                account = account,
                                myProfile = myProfile,
                                directoryProfiles = directoryProfiles,
                                chatsState = chatsState,
                                statusesState = statusesState,
                                callsState = callsState,
                                communitiesState = communitiesState,
                                viewedStatusIds = viewedStatusIds,
                                onOpenChat = { chatId ->
                                    navController.navigate(SmileRoutes.chat(chatId))
                                },
                                onOpenNewChat = {
                                    sessionVm.clearSearchResult()
                                    navController.navigate(SmileRoutes.NEW_CHAT)
                                },
                                onOpenNewGroup = {
                                    navController.navigate(SmileRoutes.NEW_GROUP)
                                },
                                onOpenSettings = {
                                    navController.navigate(SmileRoutes.SETTINGS)
                                },
                                onOpenQrCode = {
                                    navController.navigate(SmileRoutes.QR_CODE)
                                },
                                onSwitchLanguage = {
                                    sessionVm.setLanguage(if (account?.language == "hi") "en" else "hi")
                                },
                                onTogglePinChat = { id ->
                                    sessionVm.togglePinnedChat(id, account?.pinnedChatIds ?: emptyList())
                                },
                                onToggleArchiveChat = { id ->
                                    sessionVm.toggleArchivedChat(id, account?.archivedChatIds ?: emptyList())
                                },
                                onToggleMuteChat = { id ->
                                    sessionVm.toggleMutedChat(id, account?.mutedChatIds ?: emptyList())
                                },
                                onJoinGroupByCode = { code ->
                                    homeVm.joinGroupByCode(code) { chatId ->
                                        navController.navigate(SmileRoutes.chat(chatId))
                                    }
                                },
                                onPostStatus = { type, text, bg, caption, media, privacy ->
                                    val contactUids = directoryProfiles.map { it.userId }
                                    homeVm.postStatus(
                                        myProfile.displayName,
                                        myProfile.photoBase64,
                                        type, text, bg, caption, media, privacy, contactUids
                                    )
                                },
                                onMarkStatusViewed = { st ->
                                    homeVm.markStatusViewed(st, myProfile.displayName)
                                },
                                onMuteStatus = { id -> homeVm.muteStatus(id) },
                                onDeleteStatus = { id -> homeVm.deleteStatus(id) },
                                onReplyToStatus = { ownerUid, _ ->
                                    homeVm.startDirectChat(ownerUid) { chatId ->
                                        navController.navigate(SmileRoutes.chat(chatId))
                                    }
                                },
                                onStartCall = { callees, chatId, callType ->
                                    homeVm.startCall(myProfile.displayName, callees, chatId, callType) {}
                                },
                                onCreateCommunity = { name, desc ->
                                    homeVm.createCommunity(name, desc, emptyList())
                                },
                                onLinkGroupToCommunity = { comm, chatId ->
                                    homeVm.linkGroupToCommunity(comm, chatId)
                                }
                            )
                        }

                        composable(SmileRoutes.NEW_CHAT) {
                            NewChatScreen(
                                currentUserId = currentUserId,
                                contacts = contacts,
                                directoryProfiles = directoryProfiles,
                                searchResult = searchResult,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onSearchUser = { q -> sessionVm.searchUser(q) },
                                onSyncContacts = { sessionVm.syncPhoneContacts(context) },
                                onOpenNewGroup = { navController.navigate(SmileRoutes.NEW_GROUP) },
                                onStartDirectChat = { prof ->
                                    sessionVm.addContact(prof)
                                    homeVm.startDirectChat(prof.userId) { chatId ->
                                        navController.navigate(SmileRoutes.chat(chatId)) {
                                            popUpTo(SmileRoutes.HOME)
                                        }
                                    }
                                }
                            )
                        }

                        composable(SmileRoutes.NEW_GROUP) {
                            CreateGroupScreen(
                                currentUserId = currentUserId,
                                directoryProfiles = directoryProfiles,
                                onBack = { navController.popBackStack() },
                                onCreateGroup = { title, desc, members, photo ->
                                    homeVm.createGroup(title, desc, members, photo) { chatId ->
                                        navController.navigate(SmileRoutes.chat(chatId)) {
                                            popUpTo(SmileRoutes.HOME)
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = SmileRoutes.CHAT,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                            val chatVm: ChatDetailViewModel = viewModel(
                                key = "chat_${currentUserId}_$chatId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                        val crypto = E2eeCryptoManager(app)
                                        ChatDetailViewModel(
                                            ChatRepository(db, crypto),
                                            crypto,
                                            chatId,
                                            currentUserId
                                        )
                                    }
                                }
                            )
                            val allChats = (chatsState as? UiState.Success)?.data ?: emptyList()
                            ChatScreen(
                                viewModel = chatVm,
                                myName = myProfile.displayName,
                                wallpaperId = account?.wallpaperId ?: "default",
                                directoryProfiles = directoryProfiles,
                                allChats = allChats,
                                onBack = { navController.popBackStack() },
                                onStartCall = { callees, cid, callType ->
                                    homeVm.startCall(myProfile.displayName, callees, cid, callType) {}
                                },
                                onBlockUser = { uid, name -> sessionVm.blockUser(uid, name) },
                                onReportUser = { uid, reason, details ->
                                    sessionVm.submitReport(uid, reason, details, "USER_ABUSE")
                                }
                            )
                        }

                        composable(SmileRoutes.SETTINGS) {
                            SettingsScreen(
                                account = account,
                                profile = myProfile,
                                blockedUsers = blockedUsers,
                                onBack = { navController.popBackStack() },
                                onOpenQrCode = { navController.navigate(SmileRoutes.QR_CODE) },
                                onEditProfile = { phone, name, sid, about, photo ->
                                    sessionVm.saveProfileSetup(phone, name, sid, about, photo)
                                },
                                onUpdatePrivacy = { p, a, l, r ->
                                    sessionVm.updatePrivacy(p, a, l, r)
                                },
                                onSetDarkMode = { sessionVm.setDarkMode(it) },
                                onSetLanguage = { sessionVm.setLanguage(it) },
                                onSetWallpaper = { sessionVm.setWallpaper(it) },
                                onSetNotifications = { sessionVm.setNotificationEnabled(it) },
                                onSetDataUsage = { s, w, c -> sessionVm.setDataUsageSettings(s, w, c) },
                                onSetAppLock = { pin, bio -> sessionVm.setAppLockPin(pin, bio) },
                                onSetTwoStep = { pin -> sessionVm.setTwoStepPin(pin) },
                                onUnblockUser = { sessionVm.unblockUser(it) },
                                onSubmitTechReport = { reason, details ->
                                    sessionVm.submitReport(currentUserId, reason, details, "TECH_ISSUE")
                                },
                                onLogout = onSignOut,
                                onDeleteAccount = {
                                    sessionVm.deleteMyAccount { onSignOut() }
                                }
                            )
                        }

                        composable(SmileRoutes.QR_CODE) {
                            ProfileQrScreen(
                                profile = myProfile,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
