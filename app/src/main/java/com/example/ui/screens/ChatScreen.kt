package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.LocalSmileStrings
import com.example.core.MediaUtils
import com.example.core.VoiceNotePlayer
import com.example.core.VoiceNoteRecorder
import com.example.data.model.CallType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.example.ui.components.ReadReceiptTicks
import com.example.ui.components.SmileAvatar
import com.example.ui.components.formatChatTimestamp
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenOnline
import com.example.ui.theme.SmileLightBorder
import com.example.ui.theme.SmileMutedText
import com.example.ui.theme.SmileOffWhite
import com.example.ui.theme.SmileRedCallEnd
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowBubbleOutgoing
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.ChatDetailViewModel
import com.example.ui.viewmodel.UiState
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ChatScreen(
    viewModel: ChatDetailViewModel,
    myName: String,
    wallpaperId: String,
    directoryProfiles: List<UserProfile>,
    allChats: List<ChatRoom>,
    onBack: () -> Unit,
    onStartCall: (List<String>, String, CallType) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
    val editingTarget by viewModel.editingTarget.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()

    val chat = (chatState as? UiState.Success)?.data
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    val profileMap = remember(directoryProfiles) { directoryProfiles.associateBy { it.userId } }
    val isGroup = chat?.chatType == ChatType.GROUP.name
    val peerUid = if (!isGroup) chat?.memberUids?.firstOrNull { it != viewModel.currentUserId } else null
    val peerProfile = peerUid?.let { profileMap[it] }
    val chatTitle = if (chat != null) resolveChatDisplayTitle(chat, viewModel.currentUserId, profileMap) else "Chat"
    val peerPubKey = peerProfile?.ecdhPublicKey

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiTray by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showStarredOnly by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showLocationConsent by remember { mutableStateOf(false) }
    var showContactShareDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }

    val recorder = remember(context) { VoiceNoteRecorder(context) }
    val player = remember(context) { VoiceNotePlayer(context) }
    var playingMsgId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cleanup()
            player.stop()
            viewModel.setTyping(false)
        }
    }

    LaunchedEffect(editingTarget) {
        editingTarget?.let { inputText = it.second }
    }

    LaunchedEffect(chat?.id, messages.size) {
        if (chat != null && messages.isNotEmpty()) {
            viewModel.markAsRead(chat, messages)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Photo Picker (Zero-permission Play-compliant)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && chat != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 640)?.let { b64 ->
                viewModel.sendMediaOrRichMessage(
                    chat = chat,
                    senderName = myName,
                    peerPubKey = peerPubKey,
                    messageType = MessageType.IMAGE,
                    textBody = "Photo",
                    mediaBase64 = b64
                )
            }
        }
    }

    // Document Picker (SAF)
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && chat != null) {
            val (name, mime, b64) = MediaUtils.readDocumentInfo(context, uri)
            viewModel.sendMediaOrRichMessage(
                chat = chat,
                senderName = myName,
                peerPubKey = peerPubKey,
                messageType = MessageType.DOCUMENT,
                textBody = name,
                mediaBase64 = b64 ?: "",
                fileName = name,
                fileMimeType = mime
            )
        }
    }

    // Audio Permission for Voice Note
    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = recorder.startRecording()
        }
    }

    // Coarse Location Permission
    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && chat != null) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    viewModel.sendMediaOrRichMessage(
                        chat = chat,
                        senderName = myName,
                        peerPubKey = peerPubKey,
                        messageType = MessageType.LOCATION,
                        textBody = "Shared Location",
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }
            }
        }
    }

    val wallpaperColor = when (wallpaperId) {
        "warm_cream" -> Color(0xFFFFFDF0)
        "soft_yellow" -> SmileYellowSoft
        "mint_breeze" -> Color(0xFFF0FDF4)
        "dark_slate" -> Color(0xFF181A1E)
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = wallpaperColor,
        topBar = {
            Surface(color = SmileYellow, shadowElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileBlack)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showInfoDialog = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmileAvatar(
                                name = chatTitle,
                                photoBase64 = if (isGroup) (chat?.photoBase64 ?: "") else (peerProfile?.photoBase64 ?: ""),
                                size = 40.dp,
                                isGroup = isGroup,
                                isOnline = !isGroup && (peerProfile?.online == true)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = chatTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmileBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val typingOthers = chat?.typingUids?.any { it != viewModel.currentUserId } == true
                                val sub = when {
                                    typingOthers -> strings.typing
                                    isGroup -> "${chat?.memberUids?.size ?: 0} members"
                                    peerProfile?.online == true -> strings.online
                                    else -> strings.offline
                                }
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (typingOthers || peerProfile?.online == true) Color(0xFF15803D) else SmileBlack.copy(alpha = 0.7f)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (chat != null) {
                                    val callees = chat.memberUids.filter { it != viewModel.currentUserId }
                                    if (callees.isNotEmpty()) {
                                        onStartCall(
                                            callees,
                                            chat.id,
                                            if (isGroup) CallType.GROUP_VIDEO else CallType.VIDEO
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.testTag("chat_video_call_btn")
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = strings.videoCall, tint = SmileBlack)
                        }

                        IconButton(
                            onClick = {
                                if (chat != null) {
                                    val callees = chat.memberUids.filter { it != viewModel.currentUserId }
                                    if (callees.isNotEmpty()) {
                                        onStartCall(
                                            callees,
                                            chat.id,
                                            if (isGroup) CallType.GROUP_VOICE else CallType.VOICE
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.testTag("chat_voice_call_btn")
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = strings.voiceCall, tint = SmileBlack)
                        }

                        Box {
                            IconButton(onClick = { showOverflow = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = SmileBlack)
                            }
                            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isGroup) "Group Info & Members" else "Contact & Safety Number") },
                                    onClick = { showOverflow = false; showInfoDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search Messages") },
                                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                                    onClick = { showOverflow = false; showSearchBar = !showSearchBar }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showStarredOnly) "Show All Messages" else "Starred Messages") },
                                    leadingIcon = { Icon(Icons.Filled.Star, null) },
                                    onClick = { showOverflow = false; showStarredOnly = !showStarredOnly }
                                )
                                DropdownMenuItem(
                                    text = { Text("Disappearing Messages") },
                                    leadingIcon = { Icon(Icons.Filled.Timer, null) },
                                    onClick = { showOverflow = false; showDisappearingDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Chat") },
                                    onClick = {
                                        showOverflow = false
                                        if (chat != null) {
                                            val exported = viewModel.exportChatText(chat, messages, peerPubKey)
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, exported)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Export SMILE Chat"))
                                        }
                                    }
                                )
                                if (!isGroup && peerUid != null) {
                                    DropdownMenuItem(
                                        text = { Text("Block User") },
                                        onClick = {
                                            showOverflow = false
                                            onBlockUser(peerUid, chatTitle)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Report User") },
                                        onClick = {
                                            showOverflow = false
                                            onReportUser(peerUid, "Abuse / Spam", "Reported from chat ${chat?.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showSearchBar) {
                        OutlinedTextField(
                            value = chatSearchQuery,
                            onValueChange = { chatSearchQuery = it },
                            placeholder = { Text("Search in conversation...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showSearchBar = false; chatSearchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .background(SmileWhite, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (errorBanner != null) {
                Surface(color = SmileRedCallEnd.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(errorBanner ?: "", color = SmileRedCallEnd, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearErrorBanner() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            if (chat == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SmileYellow)
                }
            } else {
                val filteredMessages = remember(messages, chatSearchQuery, showStarredOnly, chat, peerPubKey) {
                    messages.filter { msg ->
                        val starMatch = !showStarredOnly || msg.starredByUids.contains(viewModel.currentUserId)
                        val queryMatch = if (chatSearchQuery.isBlank()) true else {
                            viewModel.decryptMessage(chat, msg, peerPubKey).contains(chatSearchQuery, ignoreCase = true)
                        }
                        starMatch && queryMatch
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = SmileBlack, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.e2eeBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SmileBlack
                                )
                            }
                        }
                    }

                    items(filteredMessages, key = { it.id }) { msg ->
                        val isMine = msg.senderId == viewModel.currentUserId
                        val decryptedText = remember(msg.id, msg.ciphertext, msg.deletedForEveryone) {
                            viewModel.decryptMessage(chat, msg, peerPubKey)
                        }
                        MessageBubble(
                            msg = msg,
                            decryptedText = decryptedText,
                            isMine = isMine,
                            isGroup = isGroup,
                            currentUserId = viewModel.currentUserId,
                            isPlayingVoice = playingMsgId == msg.id,
                            onToggleVoicePlay = {
                                if (playingMsgId == msg.id) {
                                    player.stop()
                                    playingMsgId = null
                                } else {
                                    playingMsgId = msg.id
                                    player.playBase64(msg.mediaBase64) { playingMsgId = null }
                                }
                            },
                            onOpenLocation = { lat, lng ->
                                val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(SMILE+Location)")
                                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                            },
                            onLongPress = { selectedMessage = msg }
                        )
                    }
                }

                // Reply or Edit bar
                val activeBanner = editingTarget?.let { "Editing: ${it.second}" }
                    ?: replyTarget?.let { "Replying to: ${it.second}" }
                if (activeBanner != null) {
                    Surface(color = SmileYellowSoft, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeBanner,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                color = SmileBlack
                            )
                            IconButton(onClick = {
                                viewModel.clearReplyTarget()
                                viewModel.clearEditingTarget()
                                inputText = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = SmileBlack)
                            }
                        }
                    }
                }

                // Emoji / Sticker Tray
                AnimatedVisibility(visible = showEmojiTray) {
                    val stickers = listOf("😊", "😂", "❤️", "🙏", "👍", "🎉", "🔥", "🌟", "😎", "🥳")
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(stickers) { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.sendMediaOrRichMessage(
                                                chat = chat,
                                                senderName = myName,
                                                peerPubKey = peerPubKey,
                                                messageType = MessageType.STICKER,
                                                textBody = emoji
                                            )
                                            showEmojiTray = false
                                        }
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }

                val canSend = !chat.onlyAdminsCanSend ||
                    chat.adminUids.contains(viewModel.currentUserId) ||
                    chat.coAdminUids.contains(viewModel.currentUserId)

                if (!canSend) {
                    Surface(color = SmileYellowSoft, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Only group admins can send messages in this chat.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmileBlack
                        )
                    }
                } else {
                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEmojiTray = !showEmojiTray }) {
                            Icon(Icons.Filled.EmojiEmotions, contentDescription = "Emoji & Stickers")
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.setTyping(it.isNotBlank())
                            },
                            placeholder = { Text(if (isRecording) "Recording voice note..." else strings.typeMessageHint) },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier.testTag("chat_attach_button")
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Attach")
                        }

                        if (inputText.isNotBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = SmileYellow,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        val toSend = inputText
                                        inputText = ""
                                        viewModel.setTyping(false)
                                        viewModel.sendOrEditTextMessage(chat, myName, peerPubKey, toSend)
                                    }
                                    .testTag("chat_send_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = SmileBlack)
                                }
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (isRecording) SmileRedCallEnd else SmileYellow,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (isRecording) {
                                            isRecording = false
                                            val result = recorder.stopAndEncode()
                                            if (result != null) {
                                                viewModel.sendMediaOrRichMessage(
                                                    chat = chat,
                                                    senderName = myName,
                                                    peerPubKey = peerPubKey,
                                                    messageType = MessageType.VOICE,
                                                    textBody = "Voice Note",
                                                    mediaBase64 = result.first,
                                                    durationSec = result.second.toLong()
                                                )
                                            }
                                        } else {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) {
                                                isRecording = recorder.startRecording()
                                            } else {
                                                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .testTag("chat_voice_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = if (isRecording) "Stop recording" else "Record voice note",
                                        tint = if (isRecording) SmileWhite else SmileBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAttachmentSheet && chat != null) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Share in SMILE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AttachmentTile(Icons.Filled.Image, "Photo") {
                        showAttachmentSheet = false
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    AttachmentTile(Icons.Filled.Description, "Document") {
                        showAttachmentSheet = false
                        docPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "*/*"))
                    }
                    AttachmentTile(Icons.Filled.LocationOn, "Location") {
                        showAttachmentSheet = false
                        showLocationConsent = true
                    }
                    AttachmentTile(Icons.Filled.Person, "Contact") {
                        showAttachmentSheet = false
                        showContactShareDialog = true
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showLocationConsent) {
        AlertDialog(
            onDismissRequest = { showLocationConsent = false },
            title = { Text("Share Location?") },
            text = { Text("SMILE will share your approximate current location with members of this chat only.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationConsent = false
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted && chat != null) {
                            LocationServices.getFusedLocationProviderClient(context).lastLocation
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        viewModel.sendMediaOrRichMessage(
                                            chat = chat,
                                            senderName = myName,
                                            peerPubKey = peerPubKey,
                                            messageType = MessageType.LOCATION,
                                            textBody = "Shared Location",
                                            latitude = loc.latitude,
                                            longitude = loc.longitude
                                        )
                                    }
                                }
                        } else {
                            locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationConsent = false }) { Text("Cancel") }
            }
        )
    }

    if (showContactShareDialog && chat != null) {
        AlertDialog(
            onDismissRequest = { showContactShareDialog = false },
            title = { Text("Share SMILE Contact") },
            text = {
                Column {
                    directoryProfiles.filter { it.userId != viewModel.currentUserId }.take(10).forEach { prof ->
                        TextButton(onClick = {
                            viewModel.sendMediaOrRichMessage(
                                chat = chat,
                                senderName = myName,
                                peerPubKey = peerPubKey,
                                messageType = MessageType.CONTACT,
                                textBody = prof.displayName,
                                sharedContactName = prof.displayName,
                                sharedContactPhone = "@${prof.smileId}"
                            )
                            showContactShareDialog = false
                        }) {
                            Text("${prof.displayName} (@${prof.smileId})")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showContactShareDialog = false }) { Text("Close") } }
        )
    }

    // Message Context Actions
    selectedMessage?.let { msg ->
        val isMine = msg.senderId == viewModel.currentUserId
        val plain = chat?.let { viewModel.decryptMessage(it, msg, peerPubKey) } ?: ""
        AlertDialog(
            onDismissRequest = { selectedMessage = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("😊", "❤️", "👍", "😂", "🙏").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.toggleReaction(msg, emoji)
                                        selectedMessage = null
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                    TextButton(onClick = {
                        viewModel.setReplyTarget(msg.id, plain.take(60))
                        selectedMessage = null
                    }) { Text("Reply") }
                    TextButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("SMILE message", plain))
                        selectedMessage = null
                    }) { Text("Copy") }
                    TextButton(onClick = {
                        viewModel.toggleStar(msg)
                        selectedMessage = null
                    }) { Text(if (msg.starredByUids.contains(viewModel.currentUserId)) "Unstar" else "Star Message") }
                    TextButton(onClick = {
                        forwardMessage = msg
                        selectedMessage = null
                    }) { Text("Forward") }
                    if (isMine && !msg.deletedForEveryone && msg.messageType == MessageType.TEXT.name) {
                        TextButton(onClick = {
                            viewModel.setEditingTarget(msg.id, plain)
                            selectedMessage = null
                        }) { Text("Edit Message") }
                    }
                    TextButton(onClick = {
                        viewModel.deleteForMe(msg.id)
                        selectedMessage = null
                    }) { Text("Delete for Me") }
                    if (isMine && !msg.deletedForEveryone) {
                        TextButton(onClick = {
                            viewModel.deleteForEveryone(msg.id)
                            selectedMessage = null
                        }) { Text("Delete for Everyone", color = SmileRedCallEnd) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessage = null }) { Text("Close") }
            }
        )
    }

    // Forward Dialog
    forwardMessage?.let { msg ->
        val plain = chat?.let { viewModel.decryptMessage(it, msg, peerPubKey) } ?: ""
        AlertDialog(
            onDismissRequest = { forwardMessage = null },
            title = { Text("Forward to Chat") },
            text = {
                Column {
                    allChats.filter { it.id != chat?.id }.take(10).forEach { target ->
                        val label = resolveChatDisplayTitle(target, viewModel.currentUserId, profileMap)
                        TextButton(onClick = {
                            val targetPeerKey = if (target.chatType == ChatType.DIRECT.name) {
                                target.memberUids.firstOrNull { it != viewModel.currentUserId }?.let { profileMap[it]?.ecdhPublicKey }
                            } else null
                            viewModel.sendMediaOrRichMessage(
                                chat = target,
                                senderName = myName,
                                peerPubKey = targetPeerKey,
                                messageType = MessageType.fromRaw(msg.messageType),
                                textBody = plain,
                                mediaBase64 = msg.mediaBase64,
                                forwarded = true
                            )
                            forwardMessage = null
                        }) { Text(label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { forwardMessage = null }) { Text("Cancel") } }
        )
    }

    // Disappearing Messages Dialog
    if (showDisappearingDialog && chat != null) {
        val options = listOf(0L to "Off", 86400L to "24 Hours", 604800L to "7 Days", 7776000L to "90 Days")
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages") },
            text = {
                Column {
                    options.forEach { (secs, label) ->
                        TextButton(onClick = {
                            viewModel.setDisappearingTimer(secs)
                            showDisappearingDialog = false
                        }) {
                            Text(if (chat.disappearingSeconds == secs) "✓ $label" else label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDisappearingDialog = false }) { Text("Close") } }
        )
    }

    // Chat / Group Info & Safety Number Dialog
    if (showInfoDialog && chat != null) {
        var groupTitle by remember(chat.title) { mutableStateOf(chat.title) }
        var groupDesc by remember(chat.description) { mutableStateOf(chat.description) }
        var adminsOnly by remember(chat.onlyAdminsCanSend) { mutableStateOf(chat.onlyAdminsCanSend) }
        val isAdmin = chat.adminUids.contains(viewModel.currentUserId) || chat.coAdminUids.contains(viewModel.currentUserId)

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(chatTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isGroup) {
                        Text("Smile ID: @${peerProfile?.smileId ?: "member"}", fontWeight = FontWeight.SemiBold)
                        Text("About: ${peerProfile?.about ?: ""}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("E2EE Safety Number:", fontWeight = FontWeight.Bold)
                        Text(
                            text = viewModel.computeSafetyNumber(peerPubKey),
                            style = MaterialTheme.typography.labelMedium,
                            color = SmileGreenOnline
                        )
                    } else {
                        if (chat.inviteCode.isNotBlank()) {
                            Text("Group Invite Code: ${chat.inviteCode}", fontWeight = FontWeight.Bold)
                        }
                        if (isAdmin) {
                            OutlinedTextField(
                                value = groupTitle,
                                onValueChange = { groupTitle = it },
                                label = { Text("Group Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = groupDesc,
                                onValueChange = { groupDesc = it },
                                label = { Text("Group Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Only admins can send", modifier = Modifier.weight(1f))
                                Switch(checked = adminsOnly, onCheckedChange = { adminsOnly = it })
                            }
                        } else {
                            Text(chat.description.ifBlank { "No group description" })
                        }
                        Text("Members (${chat.memberUids.size}):", fontWeight = FontWeight.Bold)
                        chat.memberUids.take(8).forEach { mUid ->
                            val name = profileMap[mUid]?.displayName ?: if (mUid == viewModel.currentUserId) "You" else mUid.take(8)
                            val role = if (chat.adminUids.contains(mUid)) " (Admin)" else ""
                            Text("• $name$role", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                if (isGroup && isAdmin) {
                    Button(
                        onClick = {
                            viewModel.updateGroupSettings(
                                groupTitle, groupDesc, adminsOnly, chat.memberUids, chat.coAdminUids
                            )
                            showInfoDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                    ) { Text("Save") }
                } else {
                    TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
                }
            },
            dismissButton = {
                if (isGroup && isAdmin) {
                    TextButton(onClick = { showInfoDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun AttachmentTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = SmileYellow,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = SmileBlack)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: ChatMessage,
    decryptedText: String,
    isMine: Boolean,
    isGroup: Boolean,
    currentUserId: String,
    isPlayingVoice: Boolean,
    onToggleVoicePlay: () -> Unit,
    onOpenLocation: (Double, Double) -> Unit,
    onLongPress: () -> Unit
) {
    val bubbleColor = if (isMine) SmileYellowBubbleOutgoing else SmileWhite
    val align = if (isMine) Alignment.End else Alignment.Start
    val isRead = msg.readByUids.any { it != currentUserId }
    val isDelivered = msg.deliveredToUids.any { it != currentUserId }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .border(
                    width = 0.8.dp,
                    color = if (isMine) SmileYellow else SmileLightBorder,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isMine) 18.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 18.dp
                    )
                )
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isGroup && !isMine) {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
                if (msg.forwarded) {
                    Text(
                        text = "↪ Forwarded",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmileMutedText
                    )
                }
                if (msg.replyToSnippet.isNotBlank()) {
                    Surface(
                        color = SmileOffWhite,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = msg.replyToSnippet,
                            style = MaterialTheme.typography.labelSmall,
                            color = SmileMutedText,
                            maxLines = 2,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                when {
                    msg.deletedForEveryone -> {
                        Text("🚫 This message was deleted", color = SmileMutedText, style = MaterialTheme.typography.bodyMedium)
                    }
                    msg.messageType == MessageType.STICKER.name -> {
                        Text(decryptedText, fontSize = 42.sp)
                    }
                    msg.messageType == MessageType.IMAGE.name && msg.mediaBase64.isNotBlank() -> {
                        val bmp = remember(msg.mediaBase64) { MediaUtils.decodeBase64ToBitmap(msg.mediaBase64) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    msg.messageType == MessageType.VOICE.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onToggleVoicePlay)
                        ) {
                            Surface(shape = CircleShape, color = SmileYellow, modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlayingVoice) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = SmileBlack
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Voice Note • ${msg.durationSec}s", color = SmileBlack, fontWeight = FontWeight.Medium)
                        }
                    }
                    msg.messageType == MessageType.DOCUMENT.name -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = SmileBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg.fileName.ifBlank { "Document" }, color = SmileBlack, fontWeight = FontWeight.Medium)
                        }
                    }
                    msg.messageType == MessageType.LOCATION.name -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenLocation(msg.latitude, msg.longitude) }
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SmileRedCallEnd)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📍 Shared Location (${String.format("%.4f", msg.latitude)}, ${String.format("%.4f", msg.longitude)})",
                                color = SmileBlack,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    msg.messageType == MessageType.CONTACT.name -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = SmileBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(msg.sharedContactName, color = SmileBlack, fontWeight = FontWeight.Bold)
                                Text(msg.sharedContactPhone, color = SmileMutedText, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = decryptedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SmileBlack
                        )
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (msg.reactions.isNotEmpty()) {
                        Text(msg.reactions.values.distinct().joinToString(""), fontSize = 13.sp)
                    }
                    if (msg.starredByUids.contains(currentUserId)) {
                        Icon(Icons.Filled.Star, contentDescription = "Starred", tint = SmileBlack, modifier = Modifier.size(12.dp))
                    }
                    if (msg.edited) {
                        Text("edited", style = MaterialTheme.typography.labelSmall, color = SmileMutedText)
                    }
                    Text(
                        text = formatChatTimestamp(msg.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = SmileMutedText
                    )
                    if (isMine) {
                        ReadReceiptTicks(isRead = isRead, isDelivered = isDelivered)
                    }
                }
            }
        }
    }
}
