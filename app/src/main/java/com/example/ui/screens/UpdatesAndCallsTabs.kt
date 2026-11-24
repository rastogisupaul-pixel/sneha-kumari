package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.LocalSmileStrings
import com.example.core.MediaUtils
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.Community
import com.example.data.model.StatusPrivacy
import com.example.data.model.StatusType
import com.example.data.model.StatusUpdate
import com.example.data.model.UserProfile
import com.example.ui.components.SmileAvatar
import com.example.ui.components.formatChatTimestamp
import com.example.ui.components.formatDuration
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenOnline
import com.example.ui.theme.SmileMutedText
import com.example.ui.theme.SmileRedCallEnd
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.example.ui.viewmodel.UiState

@Composable
fun UpdatesTabBody(
    currentUserId: String,
    myProfile: UserProfile?,
    statusesState: UiState<List<StatusUpdate>>,
    viewedStatusIds: Set<String>,
    onAddStatusClick: () -> Unit,
    onMarkViewed: (StatusUpdate) -> Unit,
    onMuteStatus: (String) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onReplyToStatus: (String, String) -> Unit
) {
    val strings = LocalSmileStrings.current
    var activeStatus by remember { mutableStateOf<StatusUpdate?>(null) }

    when (statusesState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(statusesState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val allStatuses = statusesState.data
            val myStatuses = allStatuses.filter { it.userId == currentUserId }
            val otherStatuses = allStatuses.filter { it.userId != currentUserId }
            val recent = otherStatuses.filter { !viewedStatusIds.contains(it.id) }
            val viewed = otherStatuses.filter { viewedStatusIds.contains(it.id) }

            LazyColumn(
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (myStatuses.isNotEmpty()) {
                                    activeStatus = myStatuses.first()
                                } else {
                                    onAddStatusClick()
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            SmileAvatar(
                                name = myProfile?.displayName ?: "Me",
                                photoBase64 = myProfile?.photoBase64 ?: "",
                                size = 56.dp
                            )
                            Surface(
                                shape = CircleShape,
                                color = SmileYellow,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { onAddStatusClick() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add status", tint = SmileBlack, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.myStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (myStatuses.isEmpty()) strings.tapToAddStatus else "${myStatuses.size} active update(s) • 24h auto-expire",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (myStatuses.isNotEmpty()) {
                            IconButton(onClick = onAddStatusClick) {
                                Icon(Icons.Filled.Add, contentDescription = "Add another status")
                            }
                        }
                    }
                }

                if (recent.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.recentUpdates,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(recent, key = { it.id }) { status ->
                        StatusListRow(status = status, isViewed = false) {
                            onMarkViewed(status)
                            activeStatus = status
                        }
                    }
                }

                if (viewed.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.viewedUpdates,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(viewed, key = { it.id }) { status ->
                        StatusListRow(status = status, isViewed = true) {
                            activeStatus = status
                        }
                    }
                }

                if (otherStatuses.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(
                                text = "Share text or photo status updates with your SMILE contacts. Updates automatically disappear after 24 hours.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmileBlack,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    activeStatus?.let { st ->
        StatusViewerDialog(
            status = st,
            isMine = st.userId == currentUserId,
            onDismiss = { activeStatus = null },
            onDelete = {
                onDeleteStatus(st.id)
                activeStatus = null
            },
            onMute = {
                onMuteStatus(st.id)
                activeStatus = null
            },
            onReply = { replyText ->
                onReplyToStatus(st.userId, "Replied to status: $replyText")
                activeStatus = null
            }
        )
    }
}

@Composable
private fun StatusListRow(
    status: StatusUpdate,
    isViewed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .border(
                    width = 2.5.dp,
                    color = if (isViewed) SmileMutedText.copy(alpha = 0.4f) else SmileYellow,
                    shape = CircleShape
                )
                .padding(3.dp)
        ) {
            SmileAvatar(
                name = status.userName,
                photoBase64 = status.userAvatarBase64,
                size = 52.dp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(status.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = formatChatTimestamp(status.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPublish: (StatusType, String, String, String, String, StatusPrivacy) -> Unit
) {
    val context = LocalContext.current
    var statusType by remember { mutableStateOf(StatusType.TEXT) }
    var textContent by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var mediaBase64 by remember { mutableStateOf("") }
    var bgHex by remember { mutableStateOf("#FFD600") }
    var privacy by remember { mutableStateOf(StatusPrivacy.EVERYONE) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 640)?.let {
                mediaBase64 = it
                statusType = StatusType.IMAGE
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Status Update (24h)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statusType == StatusType.TEXT,
                        onClick = { statusType = StatusType.TEXT },
                        label = { Text("Text") }
                    )
                    FilterChip(
                        selected = statusType == StatusType.IMAGE,
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        leadingIcon = { Icon(Icons.Filled.Image, null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Photo") }
                    )
                }

                if (statusType == StatusType.TEXT) {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it.take(500) },
                        label = { Text("Share what's on your mind...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FFD600", "#111111", "#22C55E", "#3B82F6").forEach { hex ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (bgHex == hex) 3.dp else 1.dp,
                                        color = if (bgHex == hex) SmileBlack else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { bgHex = hex }
                            )
                        }
                    }
                } else {
                    if (mediaBase64.isNotBlank()) {
                        val bmp = remember(mediaBase64) { MediaUtils.decodeBase64ToBitmap(mediaBase64) }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Selected status photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it.take(200) },
                        label = { Text("Add a caption...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Status Privacy", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPrivacy.entries.forEach { p ->
                        FilterChip(
                            selected = privacy == p,
                            onClick = { privacy = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = if (statusType == StatusType.TEXT) textContent.isNotBlank() else mediaBase64.isNotBlank()
                    if (valid) onPublish(statusType, textContent, bgHex, caption, mediaBase64, privacy)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
            ) { Text("Post Status") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StatusViewerDialog(
    status: StatusUpdate,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMute: () -> Unit,
    onReply: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    val bgColor = remember(status.bgColorHex) {
        try {
            Color(android.graphics.Color.parseColor(status.bgColorHex))
        } catch (_: Exception) {
            SmileYellow
        }
    }
    val textColor = if (status.bgColorHex.equals("#111111", ignoreCase = true)) SmileWhite else SmileBlack

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (status.statusType == StatusType.IMAGE.name) SmileBlack else bgColor)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmileAvatar(name = status.userName, photoBase64 = status.userAvatarBase64, size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.userName,
                            color = if (status.statusType == StatusType.IMAGE.name) SmileWhite else textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatChatTimestamp(status.createdAt),
                            color = (if (status.statusType == StatusType.IMAGE.name) SmileWhite else textColor).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (isMine) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete status", tint = SmileRedCallEnd)
                        }
                    } else {
                        IconButton(onClick = onMute) {
                            Icon(Icons.Filled.NotificationsOff, contentDescription = "Mute status", tint = SmileWhite)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = if (status.statusType == StatusType.IMAGE.name) SmileYellow else textColor)
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (status.statusType == StatusType.IMAGE.name) {
                        val bmp = remember(status.mediaBase64) { MediaUtils.decodeBase64ToBitmap(status.mediaBase64) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = status.caption,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                                )
                            }
                            if (status.caption.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(status.caption, color = SmileWhite, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        Text(
                            text = status.textContent,
                            color = textColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (!isMine) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply to status...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .background(SmileWhite, RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply(replyText)
                                }
                            },
                            modifier = Modifier.background(SmileYellow, CircleShape)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send reply", tint = SmileBlack)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = textColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Visible for 24 hours • Privacy: ${status.privacy}", color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTabBody(
    currentUserId: String,
    callsState: UiState<List<CallSession>>,
    profileByUid: Map<String, UserProfile>,
    onStartCall: (List<String>, String, CallType) -> Unit
) {
    val strings = LocalSmileStrings.current
    when (callsState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(callsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val calls = callsState.data
            if (calls.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = SmileYellow, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(strings.noCallsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Start a voice or video call from any chat!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(calls, key = { it.id }) { call ->
                        val isOutgoing = call.callerId == currentUserId
                        val peerUids = call.participantUids.filter { it != currentUserId }
                        val peerName = if (isOutgoing) {
                            peerUids.firstOrNull()?.let { profileByUid[it]?.displayName } ?: "SMILE Call"
                        } else {
                            call.callerName
                        }
                        val isVideo = call.callType.contains("VIDEO")
                        val isMissed = call.status == CallStatus.MISSED.name && !isOutgoing

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmileAvatar(
                                name = peerName,
                                photoBase64 = peerUids.firstOrNull()?.let { profileByUid[it]?.photoBase64 } ?: "",
                                size = 50.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMissed) SmileRedCallEnd else MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                            else -> Icons.AutoMirrored.Filled.CallReceived
                                        },
                                        contentDescription = null,
                                        tint = if (isMissed) SmileRedCallEnd else SmileGreenOnline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val durLabel = if (call.durationSeconds > 0) " • ${formatDuration(call.durationSeconds)}" else ""
                                    Text(
                                        text = "${formatChatTimestamp(call.createdAt)}$durLabel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (peerUids.isNotEmpty()) {
                                        onStartCall(
                                            peerUids,
                                            call.chatId,
                                            if (isVideo) CallType.VIDEO else CallType.VOICE
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                                    contentDescription = "Redial",
                                    tint = SmileBlack,
                                    modifier = Modifier
                                        .background(SmileYellow, CircleShape)
                                        .padding(6.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun CommunitiesTabBody(
    currentUserId: String,
    communitiesState: UiState<List<Community>>,
    allChats: List<ChatRoom>,
    onOpenChat: (String) -> Unit,
    onCreateCommunityClick: () -> Unit,
    onLinkGroup: (Community, String) -> Unit
) {
    val strings = LocalSmileStrings.current
    var linkTargetCommunity by remember { mutableStateOf<Community?>(null) }

    when (communitiesState) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SmileYellow)
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(communitiesState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val communities = communitiesState.data
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_community_banner_1795517267576),
                                contentDescription = "SMILE Communities",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = strings.noCommunitiesTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SmileBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = onCreateCommunityClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("create_community_button")
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.createCommunity, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                items(communities, key = { it.id }) { community ->
                    val linkedGroups = allChats.filter { community.linkedChatIds.contains(it.id) }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SmileYellow,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Groups, contentDescription = null, tint = SmileBlack)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(community.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (community.description.isNotBlank()) {
                                        Text(
                                            community.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            linkedGroups.forEach { group ->
                                val isAnnouncement = group.id == community.announcementChatId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenChat(group.id) }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAnnouncement) Icons.Filled.Campaign else Icons.Filled.Groups,
                                        contentDescription = null,
                                        tint = SmileBlack,
                                        modifier = Modifier
                                            .background(SmileYellowSoft, CircleShape)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(group.title.ifBlank { "Group" }, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            group.lastMessageText.ifBlank { "Tap to open" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (community.adminUids.contains(currentUserId)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { linkTargetCommunity = community },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Group to Community")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    linkTargetCommunity?.let { comm ->
        val candidateGroups = allChats.filter {
            it.chatType == ChatType.GROUP.name && !comm.linkedChatIds.contains(it.id)
        }
        AlertDialog(
            onDismissRequest = { linkTargetCommunity = null },
            title = { Text("Add Group to ${comm.name}") },
            text = {
                if (candidateGroups.isEmpty()) {
                    Text("Create a group first from the overflow menu (New Group), then link it here.")
                } else {
                    Column {
                        candidateGroups.forEach { grp ->
                            TextButton(onClick = {
                                onLinkGroup(comm, grp.id)
                                linkTargetCommunity = null
                            }) {
                                Text(grp.title.ifBlank { "SMILE Group" })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { linkTargetCommunity = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create SMILE Community") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Community Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description) },
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
