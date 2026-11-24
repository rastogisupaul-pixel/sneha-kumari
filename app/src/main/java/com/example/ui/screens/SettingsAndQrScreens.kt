package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.LocalSmileStrings
import com.example.core.MediaUtils
import com.example.data.model.BlockedUser
import com.example.data.model.PrivacyVisibility
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.components.QrCodeCanvas
import com.example.ui.components.SmileAvatar
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileRedCallEnd
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft

@Composable
fun SettingsScreen(
    account: UserAccount?,
    profile: UserProfile?,
    blockedUsers: List<BlockedUser>,
    onBack: () -> Unit,
    onOpenQrCode: () -> Unit,
    onEditProfile: (phone: String, name: String, smileId: String, about: String, photoBase64: String) -> Unit,
    onUpdatePrivacy: (PrivacyVisibility, PrivacyVisibility, PrivacyVisibility, Boolean) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetWallpaper: (String) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetDataUsage: (Boolean, Boolean, Boolean) -> Unit,
    onSetAppLock: (String, Boolean) -> Unit,
    onSetTwoStep: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSubmitTechReport: (String, String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalSmileStrings.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAppLockDialog by remember { mutableStateOf(false) }
    var showTwoStepDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SmileBlack)
                    }
                    Text(
                        text = strings.settings,
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
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmileAvatar(
                    name = profile?.displayName ?: "SMILE User",
                    photoBase64 = profile?.photoBase64 ?: "",
                    size = 64.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName ?: "SMILE User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${profile?.smileId ?: "user"} • ${account?.phoneNumber ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profile?.about ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenQrCode, modifier = Modifier.testTag("settings_qr_btn")) {
                    Icon(Icons.Filled.QrCode2, contentDescription = strings.myQrCode)
                }
                IconButton(onClick = { showEditProfileDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                }
            }

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(strings.privacySecurity) },
                supportingContent = { Text("Visibility, Read Receipts, App Lock, Two-Step, Blocked (${blockedUsers.size})") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier.clickable { showPrivacyDialog = true }
            )

            ListItem(
                headlineContent = { Text("App Lock & Fingerprint") },
                supportingContent = { Text(if ((account?.appLockPinHash ?: "").isNotBlank()) "PIN Lock Enabled" else "Off") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier.clickable { showAppLockDialog = true }
            )

            ListItem(
                headlineContent = { Text("Two-Step Verification") },
                supportingContent = { Text(if ((account?.twoStepPinHash ?: "").isNotBlank()) "Enabled" else "Set recovery PIN") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier.clickable { showTwoStepDialog = true }
            )

            ListItem(
                headlineContent = { Text(strings.darkMode) },
                leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = account?.darkMode == true,
                        onCheckedChange = onSetDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            )

            ListItem(
                headlineContent = { Text(strings.language) },
                supportingContent = { Text(if (account?.language == "hi") "हिन्दी (Hindi)" else "English") },
                leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                modifier = Modifier.clickable {
                    onSetLanguage(if (account?.language == "hi") "en" else "hi")
                }
            )

            ListItem(
                headlineContent = { Text(strings.chatWallpaper) },
                supportingContent = { Text(account?.wallpaperId ?: "default") },
                leadingContent = { Icon(Icons.Filled.Wallpaper, contentDescription = null) },
                modifier = Modifier.clickable { showWallpaperDialog = true }
            )

            ListItem(
                headlineContent = { Text(strings.notifications) },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = account?.notificationsEnabled != false,
                        onCheckedChange = onSetNotifications
                    )
                }
            )

            ListItem(
                headlineContent = { Text(strings.storageData) },
                supportingContent = {
                    Text(if (account?.dataSaverEnabled == true) "Data Saver ON" else "Auto-download on Wi-Fi")
                },
                leadingContent = { Icon(Icons.Filled.DataSaverOn, contentDescription = null) },
                modifier = Modifier.clickable { showDataDialog = true }
            )

            ListItem(
                headlineContent = { Text(strings.backupRestore) },
                supportingContent = { Text("Real-time encrypted Cloud Firestore sync active") },
                leadingContent = { Icon(Icons.Filled.Backup, contentDescription = null) },
                modifier = Modifier.clickable { showBackupDialog = true }
            )

            ListItem(
                headlineContent = { Text(strings.reportIssue) },
                leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                modifier = Modifier.clickable { showReportDialog = true }
            )

            ListItem(
                headlineContent = { Text(strings.helpSupport) },
                leadingContent = { Icon(Icons.Filled.Help, contentDescription = null) },
                modifier = Modifier.clickable { showHelpDialog = true }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(strings.logout, fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onLogout).testTag("settings_logout_btn")
            )

            ListItem(
                headlineContent = { Text(strings.deleteAccount, color = SmileRedCallEnd, fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = SmileRedCallEnd) },
                modifier = Modifier.clickable { showDeleteConfirm = true }.testTag("settings_delete_account_btn")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditProfileDialog && profile != null) {
        val context = LocalContext.current
        var name by remember { mutableStateOf(profile.displayName) }
        var about by remember { mutableStateOf(profile.about) }
        var photo by remember { mutableStateOf(profile.photoBase64) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                MediaUtils.compressImageUriToBase64(context, uri, 384)?.let { photo = it }
            }
        }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SmileAvatar(name = name, photoBase64 = photo, size = 72.dp, modifier = Modifier.clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    Text("Tap avatar to change photo", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("About") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditProfile(account?.phoneNumber ?: "+919999999999", name, profile.smileId, about, photo)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showPrivacyDialog && profile != null) {
        var photoVis by remember { mutableStateOf(PrivacyVisibility.fromRaw(profile.photoVisibility)) }
        var aboutVis by remember { mutableStateOf(PrivacyVisibility.fromRaw(profile.aboutVisibility)) }
        var lastSeenVis by remember { mutableStateOf(PrivacyVisibility.fromRaw(profile.lastSeenVisibility)) }
        var readRec by remember { mutableStateOf(profile.readReceiptsEnabled) }

        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Controls") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Profile Photo Visibility", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PrivacyVisibility.entries.forEach { v ->
                            FilterChip(selected = photoVis == v, onClick = { photoVis = v }, label = { Text(v.name) })
                        }
                    }
                    Text("About Visibility", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PrivacyVisibility.entries.forEach { v ->
                            FilterChip(selected = aboutVis == v, onClick = { aboutVis = v }, label = { Text(v.name) })
                        }
                    }
                    Text("Last Seen & Online", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PrivacyVisibility.entries.forEach { v ->
                            FilterChip(selected = lastSeenVis == v, onClick = { lastSeenVis = v }, label = { Text(v.name) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Read Receipts", modifier = Modifier.weight(1f))
                        Switch(checked = readRec, onCheckedChange = { readRec = it })
                    }
                    if (blockedUsers.isNotEmpty()) {
                        Text("Blocked Users:", fontWeight = FontWeight.Bold)
                        blockedUsers.forEach { b ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(b.displayName, modifier = Modifier.weight(1f))
                                TextButton(onClick = { onUnblockUser(b.blockedUid) }) { Text("Unblock") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePrivacy(photoVis, aboutVis, lastSeenVis, readRec)
                        showPrivacyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAppLockDialog) {
        var pin by remember { mutableStateOf("") }
        var bio by remember { mutableStateOf(account?.biometricLockEnabled == true) }
        AlertDialog(
            onDismissRequest = { showAppLockDialog = false },
            title = { Text("App Lock (4-Digit PIN & Fingerprint)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("New 4-digit PIN (leave blank to disable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Fingerprint Unlock", modifier = Modifier.weight(1f))
                        Switch(checked = bio, onCheckedChange = { bio = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetAppLock(if (pin.length == 4) pin else "", bio)
                        showAppLockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAppLockDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTwoStepDialog) {
        var pin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTwoStepDialog = false },
            title = { Text("Two-Step Verification") },
            text = {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("6-digit security PIN (blank to disable)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetTwoStep(pin)
                        showTwoStepDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTwoStepDialog = false }) { Text("Cancel") } }
        )
    }

    if (showWallpaperDialog) {
        val wallpapers = listOf("default" to "Clean White", "warm_cream" to "Warm Cream", "soft_yellow" to "SMILE Soft Yellow", "mint_breeze" to "Mint Breeze", "dark_slate" to "Dark Slate")
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text("Choose Chat Wallpaper") },
            text = {
                Column {
                    wallpapers.forEach { (id, label) ->
                        TextButton(onClick = {
                            onSetWallpaper(id)
                            showWallpaperDialog = false
                        }) {
                            Text(if (account?.wallpaperId == id) "✓ $label" else label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWallpaperDialog = false }) { Text("Close") } }
        )
    }

    if (showDataDialog) {
        var saver by remember { mutableStateOf(account?.dataSaverEnabled == true) }
        var wifi by remember { mutableStateOf(account?.autoDownloadWifi != false) }
        var cell by remember { mutableStateOf(account?.autoDownloadCellular == true) }
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            title = { Text("Storage & Data Usage") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Network Data Saver", modifier = Modifier.weight(1f))
                        Switch(checked = saver, onCheckedChange = { saver = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-download on Wi-Fi", modifier = Modifier.weight(1f))
                        Switch(checked = wifi, onCheckedChange = { wifi = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-download on Mobile Data", modifier = Modifier.weight(1f))
                        Switch(checked = cell, onCheckedChange = { cell = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetDataUsage(saver, wifi, cell)
                        showDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDataDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Chat Backup & Restore") },
            text = {
                Text("Your SMILE chats, contacts, and preferences are automatically backed up and synced across devices via Cloud Firestore under your authenticated Google account.")
            },
            confirmButton = { TextButton(onClick = { showBackupDialog = false }) { Text("OK") } }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(strings.reportIssue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Issue Summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.isNotBlank()) {
                            onSubmitTechReport(reason, details)
                            showReportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileYellow, contentColor = SmileBlack)
                ) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel") } }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("SMILE Privacy Policy & Terms") },
            text = {
                Text(
                    "SMILE (बात करें, मुस्कुराएं!) encrypts 1-to-1 messages using ECDH + AES-256-GCM. Private keys never leave your device. " +
                        "Phone numbers are hashed (SHA-256) for discovery and never listed publicly. " +
                        "Contacts, Camera, Microphone, and Location permissions are requested only when you explicitly initiate those features."
                )
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Accept & Close") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete SMILE Account?") },
            text = { Text("This permanently deletes your SMILE profile, lookup indexes, and account data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmileRedCallEnd)
                ) { Text("Delete Permanently") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ProfileQrScreen(
    profile: UserProfile?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    val smileId = profile?.smileId ?: "smile_user"
    val payload = "smile://profile/@$smileId?uid=${profile?.userId ?: ""}"

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
                        text = strings.myQrCode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SmileBlack
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SmileAvatar(
                        name = profile?.displayName ?: "SMILE",
                        photoBase64 = profile?.photoBase64 ?: "",
                        size = 72.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(profile?.displayName ?: "SMILE User", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SmileBlack)
                    Text("@$smileId", style = MaterialTheme.typography.bodyMedium, color = SmileBlack.copy(alpha = 0.75f))
                    Spacer(modifier = Modifier.height(20.dp))
                    QrCodeCanvas(payload = payload, size = 210.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Connect with me on SMILE! Search my Smile ID: @$smileId")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Smile Profile"))
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Smile Profile")
                    }
                }
            }
        }
    }
}
