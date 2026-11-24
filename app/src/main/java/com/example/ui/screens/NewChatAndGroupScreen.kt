package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.LocalSmileStrings
import com.example.core.MediaUtils
import com.example.data.model.ContactEntry
import com.example.data.model.UserProfile
import com.example.ui.components.SmileAvatar
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft

@Composable
fun NewChatScreen(
    currentUserId: String,
    contacts: List<ContactEntry>,
    directoryProfiles: List<UserProfile>,
    searchResult: UserProfile?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSearchUser: (String) -> Unit,
    onSyncContacts: () -> Unit,
    onOpenNewGroup: () -> Unit,
    onStartDirectChat: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    var query by remember { mutableStateOf("") }
    var showSyncConsent by remember { mutableStateOf(false) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSyncContacts()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = SmileYellow, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileBlack)
                    }
                    Text(
                        text = strings.newChat,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SmileBlack
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Enter +91 mobile or @smileId") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("find_user_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSearchUser(query) },
                        enabled = query.isNotBlank() && !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(54.dp).testTag("find_user_button")
                    ) {
                        if (isBusy) CircularProgressIndicator(Modifier.size(20.dp), color = SmileBlack, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Search, contentDescription = "Find User")
                    }
                }
            }

            if (searchResult != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartDirectChat(searchResult) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmileAvatar(
                                name = searchResult.displayName,
                                photoBase64 = searchResult.photoBase64,
                                size = 48.dp,
                                isOnline = searchResult.online
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(searchResult.displayName, fontWeight = FontWeight.Bold, color = SmileBlack)
                                Text("@${searchResult.smileId} • ${searchResult.about}", style = MaterialTheme.typography.bodyMedium, color = SmileBlack.copy(alpha = 0.75f))
                            }
                            Text("Chat", fontWeight = FontWeight.Bold, color = SmileBlack)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenNewGroup,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("new_group_button")
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.newGroup)
                    }
                    OutlinedButton(
                        onClick = { showSyncConsent = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("sync_contacts_button")
                    ) {
                        Icon(Icons.Filled.Contacts, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.syncContacts)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join me on SMILE — बात करें, मुस्कुराएं! Safe, encrypted messaging & calls."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, strings.inviteFriends))
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.inviteFriends)
                }
            }

            if (contacts.isNotEmpty()) {
                item {
                    Text("Synced SMILE Contacts", style = MaterialTheme.typography.labelLarge)
                }
                items(contacts, key = { it.contactUid }) { c ->
                    val asProfile = UserProfile(
                        userId = c.contactUid,
                        smileId = c.smileId,
                        displayName = c.displayName,
                        about = c.about,
                        phoneHash = c.phoneHash,
                        photoBase64 = c.photoBase64,
                        ecdhPublicKey = c.ecdhPublicKey
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartDirectChat(asProfile) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmileAvatar(name = c.displayName, photoBase64 = c.photoBase64, size = 46.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.displayName, fontWeight = FontWeight.Bold)
                            Text("@${c.smileId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }

            val others = directoryProfiles.filter { it.userId != currentUserId }
            if (others.isNotEmpty()) {
                item {
                    Text("SMILE Directory", style = MaterialTheme.typography.labelLarge)
                }
                items(others, key = { it.userId }) { prof ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartDirectChat(prof) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmileAvatar(
                            name = prof.displayName,
                            photoBase64 = prof.photoBase64,
                            size = 46.dp,
                            isOnline = prof.online
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prof.displayName, fontWeight = FontWeight.Bold)
                            Text("@${prof.smileId} • ${prof.about}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (showSyncConsent) {
        AlertDialog(
            onDismissRequest = { showSyncConsent = false },
            title = { Text("Sync Phone Contacts?") },
            text = {
                Text("With your permission, SMILE securely hashes phone numbers on your device (SHA-256) to find friends already on SMILE. Your raw address book is never uploaded.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncConsent = false
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            onSyncContacts()
                        } else {
                            contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Allow & Sync") }
            },
            dismissButton = {
                TextButton(onClick = { showSyncConsent = false }) { Text("Not Now") }
            }
        )
    }
}

@Composable
fun CreateGroupScreen(
    currentUserId: String,
    directoryProfiles: List<UserProfile>,
    onBack: () -> Unit,
    onCreateGroup: (title: String, desc: String, memberUids: List<String>, photoBase64: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 384)?.let {
                photoBase64 = it
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = SmileYellow, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileBlack)
                    }
                    Text(
                        text = strings.newGroup,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SmileBlack
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.clickable {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    SmileAvatar(name = title.ifBlank { "Group" }, photoBase64 = photoBase64, size = 64.dp, isGroup = true)
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Group icon", tint = SmileBlack, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Group Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("group_name_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(300) },
                label = { Text("Group Description") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("Select Members (${selectedUids.size})", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(directoryProfiles.filter { it.userId != currentUserId }, key = { it.userId }) { prof ->
                    val checked = selectedUids.contains(prof.userId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUids = if (checked) selectedUids - prof.userId else selectedUids + prof.userId
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedUids = if (checked) selectedUids - prof.userId else selectedUids + prof.userId
                            }
                        )
                        SmileAvatar(name = prof.displayName, photoBase64 = prof.photoBase64, size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(prof.displayName, fontWeight = FontWeight.SemiBold)
                            Text("@${prof.smileId}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreateGroup(title, description, selectedUids.toList(), photoBase64)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_group_submit")
            ) {
                Text("Create Group & Generate Invite Link", fontWeight = FontWeight.Bold)
            }
        }
    }
}
