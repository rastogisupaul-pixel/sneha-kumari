package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalSmileStrings
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.Community
import com.example.data.model.StatusPrivacy
import com.example.data.model.StatusType
import com.example.data.model.StatusUpdate
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.components.SmileAvatar
import com.example.ui.components.SmileLogoMark
import com.example.ui.components.formatChatTimestamp
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenRead
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    currentUserId: String,
    account: UserAccount?,
    myProfile: UserProfile?,
    directoryProfiles: List<UserProfile>,
    chatsState: UiState<List<ChatRoom>>,
    statusesState: UiState<List<StatusUpdate>>,
    callsState: UiState<List<CallSession>>,
    communitiesState: UiState<List<Community>>,
    viewedStatusIds: Set<String>,
    onOpenChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    onOpenNewGroup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQrCode: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onTogglePinChat: (String) -> Unit,
    onToggleArchiveChat: (String) -> Unit,
    onToggleMuteChat: (String) -> Unit,
    onJoinGroupByCode: (String) -> Unit,
    onPostStatus: (StatusType, String, String, String, String, StatusPrivacy) -> Unit,
    onMarkStatusViewed: (StatusUpdate) -> Unit,
    onMuteStatus: (String) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onReplyToStatus: (String, String) -> Unit,
    onStartCall: (List<String>, String, CallType) -> Unit,
    onCreateCommunity: (String, String) -> Unit,
    onLinkGroupToCommunity: (Community, String) -> Unit
) {
    val strings = LocalSmileStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showArchivedChats by remember { mutableStateOf(false) }
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var showCreateStatusDialog by remember { mutableStateOf(false) }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    val profileByUid = remember(directoryProfiles) {
        directoryProfiles.associateBy { it.userId }
    }

    val allChats = (chatsState as? UiState.Success)?.data ?: emptyList()
    val totalUnreadChats = remember(allChats, currentUserId) {
        allChats.sumOf { (it.unreadCounts[currentUserId] ?: 0L).toInt().coerceAtLeast(0) }
    }
    val allCalls = (callsState as? UiState.Success)?.data ?: emptyList()
    val missedCallsCount = remember(allCalls, currentUserId) {
        allCalls.count { it.status == CallStatus.MISSED.name && it.callerId != currentUserId }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> onOpenNewChat()
                            1 -> showCreateStatusDialog = true
                            2 -> onOpenNewChat()
                            3 -> showCreateCommunityDialog = true
                        }
                    },
                    containerColor = SmileYellow,
                    contentColor = SmileBlack,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("home_fab")
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            1 -> Icons.Filled.Update
                            2 -> Icons.Filled.Call
                            3 -> Icons.Filled.Groups
                            else -> Icons.AutoMirrored.Filled.Chat
                        },
                        contentDescription = strings.newChat
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = SmileYellow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        SmileLogoMark(size = 42.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        val tabs = listOf(
                            Triple(0, strings.tabChats, Icons.AutoMirrored.Filled.Chat),
                            Triple(1, strings.tabUpdates, Icons.Filled.Update),
                            Triple(2, strings.tabCalls, Icons.Filled.Call),
                            Triple(3, strings.tabCommunities, Icons.Filled.Groups)
                        )
                        tabs.forEach { (idx, label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Branded Yellow Header
                    Surface(
                        color = SmileYellow,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SmileLogoMark(size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.appName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SmileBlack
                                    )
                                    Text(
                                        text = strings.tagline,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SmileBlack.copy(alpha = 0.78f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        showSearch = !showSearch
                                        if (!showSearch) searchQuery = ""
                                    },
                                    modifier = Modifier.testTag("home_search_button")
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = SmileBlack
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier.testTag("home_more_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "Menu",
                                            tint = SmileBlack
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(strings.newGroup) },
                                            leadingIcon = { Icon(Icons.Filled.Groups, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenNewGroup()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Join Group by Code") },
                                            onClick = {
                                                showOverflowMenu = false
                                                showJoinCodeDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.myQrCode) },
                                            leadingIcon = { Icon(Icons.Filled.QrCode2, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenQrCode()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.switchLanguage) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onSwitchLanguage()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.settings) },
                                            leadingIcon = { Icon(Icons.Filled.Settings, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                onOpenSettings()
                                            }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showSearch) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(strings.searchHint) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .background(SmileWhite, RoundedCornerShape(14.dp))
                                        .testTag("home_search_input")
                                )
                            }

                            if (!isWideScreen) {
                                val tabTitles = listOf(
                                    strings.tabChats,
                                    strings.tabUpdates,
                                    strings.tabCalls,
                                    strings.tabCommunities
                                )
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = SmileYellow,
                                    contentColor = SmileBlack,
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            height = 3.dp,
                                            color = SmileBlack
                                        )
                                    }
                                ) {
                                    tabTitles.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTab == index,
                                            onClick = { selectedTab = index },
                                            modifier = Modifier.testTag("tab_$index"),
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = title,
                                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (index == 0 && totalUnreadChats > 0) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        BadgedBox(badge = {
                                                            Badge(containerColor = SmileBlack, contentColor = SmileYellow) {
                                                                Text("$totalUnreadChats")
                                                            }
                                                        }) {}
                                                    } else if (index == 2 && missedCallsCount > 0) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        BadgedBox(badge = {
                                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                                Text("$missedCallsCount")
                                                            }
                                                        }) {}
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Tab Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        when (selectedTab) {
                            0 -> ChatsTabBody(
                                currentUserId = currentUserId,
                                chatsState = chatsState,
                                account = account,
                                profileByUid = profileByUid,
                                searchQuery = searchQuery,
                                showArchived = showArchivedChats,
                                onToggleShowArchived = { showArchivedChats = !showArchivedChats },
                                onOpenChat = onOpenChat,
                                onTogglePin = onTogglePinChat,
                                onToggleArchive = onToggleArchiveChat,
                                onToggleMute = onToggleMuteChat,
                                onStartNewChat = onOpenNewChat
                            )
                            1 -> UpdatesTabBody(
                                currentUserId = currentUserId,
                                myProfile = myProfile,
                                statusesState = statusesState,
                                viewedStatusIds = viewedStatusIds,
                                onAddStatusClick = { showCreateStatusDialog = true },
                                onMarkViewed = onMarkStatusViewed,
                                onMuteStatus = onMuteStatus,
                                onDeleteStatus = onDeleteStatus,
                                onReplyToStatus = onReplyToStatus
                            )
                            2 -> CallsTabBody(
                                currentUserId = currentUserId,
                                callsState = callsState,
                                profileByUid = profileByUid,
                                onStartCall = onStartCall
                            )
                            3 -> CommunitiesTabBody(
                                currentUserId = currentUserId,
                                communitiesState = communitiesState,
                                allChats = allChats,
                                onOpenChat = onOpenChat,
                                onCreateCommunityClick = { showCreateCommunityDialog = true },
                                onLinkGroup = onLinkGroupToCommunity
                            )
                        }
                    }
                }
            }
        }
    }

    if (showJoinCodeDialog) {
        AlertDialog(
            onDismissRequest = { showJoinCodeDialog = false },
            title = { Text("Join Group by Invite Code") },
            text = {
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it.uppercase() },
                    label = { Text("Invite code (e.g. SMILE-ABCD1234)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            onJoinGroupByCode(joinCodeInput)
                            joinCodeInput = ""
                            showJoinCodeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinCodeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateStatusDialog) {
        CreateStatusDialog(
            onDismiss = { showCreateStatusDialog = false },
            onPublish = { type, text, bg, caption, media, privacy ->
                onPostStatus(type, text, bg, caption, media, privacy)
                showCreateStatusDialog = false
            }
        )
    }

    if (showCreateCommunityDialog) {
        CreateCommunityDialog(
            onDismiss = { showCreateCommunityDialog = false },
            onCreate = { name, desc ->
                onCreateCommunity(name, desc)
                showCreateCommunityDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsTabBody(
    currentUserId: String,
    chatsState: UiState<List<ChatRoom>>,
    account: UserAccount?,
    profileByUid: Map<String, UserProfile>,
    searchQuery: String,
    showArchived: Boolean,
    onToggleShowArchived: () -> Unit,
    onOpenChat: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onToggleMute: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    val strings = LocalSmileStrings.current
    val pinned = account?.pinnedChatIds ?: emptyList()
    val archived = account?.archivedChatIds ?: emptyList()
    val muted = account?.mutedChatIds ?: emptyList()
    var contextChat by remember { mutableStateOf<ChatRoom?>(null) }

    when (chatsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmileYellow)
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = chatsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val allChats = chatsState.data
            val archivedCount = allChats.count { archived.contains(it.id) }
            val visibleChats = remember(allChats, showArchived, archived, pinned, searchQuery, profileByUid) {
                allChats
                    .filter { if (showArchived) archived.contains(it.id) else !archived.contains(it.id) }
                    .filter { chat ->
                        if (searchQuery.isBlank()) true
                        else {
                            val title = resolveChatDisplayTitle(chat, currentUserId, profileByUid)
                            title.contains(searchQuery, ignoreCase = true) ||
                                chat.lastMessageText.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    .sortedWith(
                        compareByDescending<ChatRoom> { pinned.contains(it.id) }
                            .thenByDescending { it.lastMessageTime?.seconds ?: 0L }
                    )
            }

            if (allChats.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_chats_1795517267563),
                        contentDescription = "Start chatting on SMILE",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.noChatsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.noChatsBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onStartNewChat,
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.newChat, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (archivedCount > 0) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(onClick = onToggleShowArchived)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Archive, contentDescription = null, tint = SmileBlack)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (showArchived) "Back to Active Chats" else "Archived ($archivedCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }

                    items(visibleChats, key = { it.id }) { chat ->
                        val isGroup = chat.chatType == ChatType.GROUP.name
                        val peerUid = if (!isGroup) chat.memberUids.firstOrNull { it != currentUserId } else null
                        val peerProfile = peerUid?.let { profileByUid[it] }
                        val title = resolveChatDisplayTitle(chat, currentUserId, profileByUid)
                        val photo = if (isGroup) chat.photoBase64 else (peerProfile?.photoBase64 ?: "")
                        val unread = (chat.unreadCounts[currentUserId] ?: 0L).toInt()
                        val isPinned = pinned.contains(chat.id)
                        val isMuted = muted.contains(chat.id)
                        val isTyping = chat.typingUids.any { it != currentUserId }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onOpenChat(chat.id) },
                                    onLongClick = { contextChat = chat }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("chat_item_${chat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmileAvatar(
                                name = title,
                                photoBase64 = photo,
                                size = 54.dp,
                                isGroup = isGroup,
                                isOnline = !isGroup && (peerProfile?.online == true)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatChatTimestamp(chat.lastMessageTime ?: chat.updatedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unread > 0) SmileGreenRead else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (chat.lastMessageSenderId == currentUserId && !isTyping) {
                                        Icon(
                                            imageVector = Icons.Filled.DoneAll,
                                            contentDescription = null,
                                            tint = SmileGreenRead,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isTyping) strings.typing else chat.lastMessageText.ifBlank { "Tap to start chatting" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isTyping) SmileGreenRead else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isPinned) {
                                        Icon(
                                            Icons.Filled.PushPin,
                                            contentDescription = "Pinned",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                        )
                                    }
                                    if (isMuted) {
                                        Icon(
                                            Icons.Filled.NotificationsOff,
                                            contentDescription = "Muted",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                        )
                                    }
                                    if (unread > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(SmileYellow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (unread > 99) "99+" else "$unread",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SmileBlack
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            modifier = Modifier.padding(start = 84.dp)
                        )
                    }
                }
            }
        }
    }

    contextChat?.let { target ->
        AlertDialog(
            onDismissRequest = { contextChat = null },
            title = { Text(resolveChatDisplayTitle(target, currentUserId, profileByUid)) },
            text = {
                Column {
                    TextButton(onClick = {
                        onTogglePin(target.id)
                        contextChat = null
                    }) {
                        Text(if (pinned.contains(target.id)) "Unpin Chat" else "Pin Chat")
                    }
                    TextButton(onClick = {
                        onToggleMute(target.id)
                        contextChat = null
                    }) {
                        Text(if (muted.contains(target.id)) "Unmute Notifications" else "Mute Chat")
                    }
                    TextButton(onClick = {
                        onToggleArchive(target.id)
                        contextChat = null
                    }) {
                        Text(if (archived.contains(target.id)) "Unarchive Chat" else "Archive Chat")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextChat = null }) { Text("Close") }
            }
        )
    }
}

fun resolveChatDisplayTitle(
    chat: ChatRoom,
    currentUserId: String,
    profileByUid: Map<String, UserProfile>
): String {
    if (chat.chatType == ChatType.GROUP.name) {
        return chat.title.ifBlank { "SMILE Group" }
    }
    val peerUid = chat.memberUids.firstOrNull { it != currentUserId }
    return peerUid?.let { profileByUid[it]?.displayName }?.takeIf { it.isNotBlank() }
        ?: chat.title.ifBlank { "SMILE Chat" }
}
