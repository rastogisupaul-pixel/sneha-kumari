package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.LocalSmileStrings
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.ui.components.SmileAvatar
import com.example.ui.components.formatDuration
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenOnline
import com.example.ui.theme.SmileRedCallEnd
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    call: CallSession,
    currentUserId: String,
    peerName: String,
    peerPhotoBase64: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalSmileStrings.current
    val isIncoming = call.callerId != currentUserId && call.status == CallStatus.RINGING.name
    val isVideoCall = call.callType.contains("VIDEO")

    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(isVideoCall) }
    var isCameraOn by remember { mutableStateOf(isVideoCall) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsedSec by remember { mutableLongStateOf(0L) }

    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val camPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasCameraPerm = it }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (isVideoCall && !hasCameraPerm) {
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isSpeaker, isMuted) {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = isSpeaker
            audioManager.isMicrophoneMute = isMuted
        } catch (_: Exception) {
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                audioManager.isMicrophoneMute = false
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(call.status) {
        if (call.status == CallStatus.ACCEPTED.name) {
            while (true) {
                delay(1000L)
                elapsedSec += 1L
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideoCall && isCameraOn) SmileBlack else Color(0xFF1A1A1A))
            .safeDrawingPadding()
    ) {
        // Real CameraX Preview for Video Calls
        if (isVideoCall && isCameraOn && hasCameraPerm) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx)
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val selector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 28.dp)) {
                SmileAvatar(
                    name = peerName,
                    photoBase64 = peerPhotoBase64,
                    size = 104.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = peerName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SmileWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                val statusLabel = when {
                    isIncoming -> strings.incomingCall
                    call.status == CallStatus.ACCEPTED.name -> formatDuration(elapsedSec)
                    else -> strings.outgoingCall
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = SmileYellow
                )
                Text(
                    text = "🔒 End-to-End Encrypted • WebRTC Ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = SmileWhite.copy(alpha = 0.7f)
                )
            }

            if (isIncoming) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = SmileRedCallEnd,
                            modifier = Modifier.size(72.dp).testTag("call_decline_btn")
                        ) {
                            IconButton(onClick = onReject, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Filled.CallEnd, contentDescription = strings.decline, tint = SmileWhite, modifier = Modifier.size(34.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(strings.decline, color = SmileWhite)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = SmileGreenOnline,
                            modifier = Modifier.size(72.dp).testTag("call_accept_btn")
                        ) {
                            IconButton(onClick = onAccept, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Filled.Call, contentDescription = strings.answer, tint = SmileWhite, modifier = Modifier.size(34.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(strings.answer, color = SmileWhite)
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = SmileBlack.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallControlButton(
                            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = strings.mute,
                            active = isMuted,
                            onClick = { isMuted = !isMuted }
                        )
                        CallControlButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = strings.speaker,
                            active = isSpeaker,
                            onClick = { isSpeaker = !isSpeaker }
                        )
                        if (isVideoCall) {
                            CallControlButton(
                                icon = if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                label = strings.camera,
                                active = isCameraOn,
                                onClick = { isCameraOn = !isCameraOn }
                            )
                            CallControlButton(
                                icon = Icons.Filled.Cameraswitch,
                                label = "Flip",
                                active = false,
                                onClick = { useFrontCamera = !useFrontCamera }
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = SmileRedCallEnd,
                            modifier = Modifier.size(58.dp).testTag("call_end_btn")
                        ) {
                            IconButton(onClick = { onEnd(elapsedSec) }, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Filled.CallEnd, contentDescription = strings.endCall, tint = SmileWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (active) SmileYellow else SmileWhite.copy(alpha = 0.2f),
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = label, tint = if (active) SmileBlack else SmileWhite)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = SmileWhite, style = MaterialTheme.typography.labelSmall)
    }
}
