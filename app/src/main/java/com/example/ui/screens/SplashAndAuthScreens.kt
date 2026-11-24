package com.example.ui.screens

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.core.LocalSmileStrings
import com.example.core.MediaUtils
import com.example.data.repository.onGoogleSignInClicked
import com.example.ui.components.SmileAvatar
import com.example.ui.components.SmileLogoMark
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenOnline
import com.example.ui.theme.SmileMutedText
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(550))
        delay(900L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileYellow)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 10.dp,
                color = SmileYellow
            ) {
                SmileLogoMark(size = 132.dp)
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "SMILE",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    letterSpacing = 3.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = SmileBlack
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "बात करें, मुस्कुराएं!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmileBlack.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val strings = LocalSmileStrings.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileWhite)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Yellow hero top wave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        SmileYellow,
                        RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
                    .padding(vertical = 44.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SmileLogoMark(size = 96.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SMILE",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SmileBlack
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.titleMedium,
                        color = SmileBlack.copy(alpha = 0.85f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.signInTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SmileBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.signInSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmileMutedText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SmileYellowSoft),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "End-to-end encryption",
                            tint = SmileGreenOnline,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ECDH + AES-256-GCM end-to-end encrypted chats, +91 mobile discovery, and private communities.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmileBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileBlack
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SmileBlack,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    initialName: String,
    isSaving: Boolean,
    errorText: String?,
    onSaveProfile: (phone: String, name: String, smileId: String, about: String, photoBase64: String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalSmileStrings.current
    var phoneDigits by remember { mutableStateOf("") }
    var displayName by remember(initialName) { mutableStateOf(initialName) }
    var smileId by remember(initialName) {
        val suggested = initialName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(14)
        mutableStateOf(if (suggested.length >= 3) suggested else "smile_user")
    }
    var about by remember { mutableStateOf("Hey there! I am using SMILE 😊") }
    var photoBase64 by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaUtils.compressImageUriToBase64(context, uri, maxDimension = 384)?.let {
                photoBase64 = it
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileWhite)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SmileYellow)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmileLogoMark(size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.setupProfileTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SmileBlack
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.labelMedium,
                            color = SmileBlack.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("setup_photo_button"),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    SmileAvatar(
                        name = displayName.ifBlank { "SMILE" },
                        photoBase64 = photoBase64,
                        size = 96.dp
                    )
                    Surface(
                        shape = CircleShape,
                        color = SmileYellow,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Pick profile photo",
                                tint = SmileBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = phoneDigits,
                    onValueChange = { input ->
                        phoneDigits = input.filter { it.isDigit() }.take(10)
                    },
                    label = { Text(strings.phoneLabel) },
                    prefix = { Text("+91 ", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_phone_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    label = { Text(strings.displayNameLabel) },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = smileId,
                    onValueChange = { input ->
                        smileId = input.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(32)
                    },
                    label = { Text(strings.smileIdLabel) },
                    leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_smile_id_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it.take(200) },
                    label = { Text(strings.aboutLabel) },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_about_input")
                )

                val displayError = validationError ?: errorText
                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when {
                            phoneDigits.length != 10 ->
                                validationError = "Please enter a valid 10-digit India mobile number."
                            displayName.isBlank() ->
                                validationError = "Please enter your display name."
                            smileId.length < 3 ->
                                validationError = "Smile ID must be at least 3 characters."
                            else -> {
                                validationError = null
                                onSaveProfile("+91$phoneDigits", displayName, smileId, about, photoBase64)
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmileYellow,
                        contentColor = SmileBlack
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("setup_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = SmileBlack,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = strings.saveAndContinue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    storedPinHash: String,
    biometricEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun triggerBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isDeviceSecure != true) {
                errorMsg = "Device biometric/lock not configured"
                return
            }
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Unlock SMILE")
                .setSubtitle("Confirm fingerprint or device credential")
                .setNegativeButton(
                    "Use PIN",
                    ContextCompat.getMainExecutor(context)
                ) { _, _ -> }
                .build()
            prompt.authenticate(
                CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onUnlockSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        errorMsg = errString?.toString()
                    }
                }
            )
        }
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            triggerBiometric()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmileYellow)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "SMILE Locked",
                tint = SmileBlack,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SMILE Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SmileBlack
            )
            Text(
                text = "Enter your 4-digit PIN to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = SmileBlack.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (idx < enteredPin.length) SmileBlack else SmileWhite)
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    row.forEach { key ->
                        Surface(
                            shape = CircleShape,
                            color = SmileWhite,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(68.dp)
                                .clickable {
                                    when (key) {
                                        "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        "BIO" -> if (biometricEnabled) triggerBiometric()
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                errorMsg = null
                                                if (enteredPin.length == 4) {
                                                    if (storedPinHash.isBlank() || onVerifyPin(enteredPin)) {
                                                        onUnlockSuccess()
                                                    } else {
                                                        enteredPin = ""
                                                        errorMsg = "Incorrect PIN. Try again."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "DEL" -> Icon(
                                        Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = SmileBlack
                                    )
                                    "BIO" -> Icon(
                                        Icons.Filled.Fingerprint,
                                        contentDescription = "Biometric unlock",
                                        tint = if (biometricEnabled) SmileBlack else SmileMutedText.copy(alpha = 0.35f)
                                    )
                                    else -> Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SmileBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (biometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { triggerBiometric() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SmileBlack)
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Fingerprint")
                }
            }
        }
    }
}
