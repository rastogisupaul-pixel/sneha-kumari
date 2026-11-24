package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.MediaUtils
import com.example.ui.theme.SmileBlack
import com.example.ui.theme.SmileGreenOnline
import com.example.ui.theme.SmileGreenRead
import com.example.ui.theme.SmileMutedText
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import com.example.ui.theme.SmileYellowSoft
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Exact SMILE brand logo: Yellow (#FFD600) rounded square, white chat bubble outline, black smiling face.
 */
@Composable
fun SmileLogoMark(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = w * 0.24f

        // Yellow rounded square background
        drawRoundRect(
            color = SmileYellow,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle crisp border on yellow backgrounds
        drawRoundRect(
            color = SmileWhite.copy(alpha = 0.25f),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = w * 0.02f)
        )

        // White chat bubble outline with bottom-left tail
        val strokeW = w * 0.095f
        val left = w * 0.17f
        val right = w * 0.83f
        val top = h * 0.20f
        val bottom = h * 0.74f
        val r = w * 0.14f

        val bubblePath = Path().apply {
            moveTo(left + r, top)
            lineTo(right - r, top)
            quadraticTo(right, top, right, top + r)
            lineTo(right, bottom - r)
            quadraticTo(right, bottom, right - r, bottom)
            lineTo(left + w * 0.26f, bottom)
            lineTo(left + w * 0.11f, h * 0.85f)
            lineTo(left + w * 0.13f, bottom - w * 0.02f)
            quadraticTo(left, bottom, left, bottom - r)
            lineTo(left, top + r)
            quadraticTo(left, top, left + r, top)
            close()
        }
        drawPath(
            path = bubblePath,
            color = SmileWhite,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Black smiling face inside the bubble
        val eyeRadius = w * 0.06f
        val eyeY = h * 0.42f
        drawCircle(
            color = SmileBlack,
            radius = eyeRadius,
            center = Offset(w * 0.39f, eyeY)
        )
        drawCircle(
            color = SmileBlack,
            radius = eyeRadius,
            center = Offset(w * 0.61f, eyeY)
        )

        val smilePath = Path().apply {
            moveTo(w * 0.36f, h * 0.54f)
            quadraticTo(w * 0.50f, h * 0.66f, w * 0.64f, h * 0.54f)
        }
        drawPath(
            path = smilePath,
            color = SmileBlack,
            style = Stroke(width = w * 0.068f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SmileAvatar(
    name: String,
    photoBase64: String,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bitmap: Bitmap? = remember(photoBase64) {
        if (photoBase64.isNotBlank()) MediaUtils.decodeBase64ToBitmap(photoBase64) else null
    }
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(SmileYellowSoft)
                .border(1.5.dp, SmileYellow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$name avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isGroup) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "$name group",
                    tint = SmileBlack,
                    modifier = Modifier.size(size * 0.55f)
                )
            } else {
                val initials = name.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "S" }
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmileBlack
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp)
                    .clip(CircleShape)
                    .background(SmileGreenOnline)
                    .border(2.dp, SmileWhite, CircleShape)
            )
        }
    }
}

@Composable
fun ReadReceiptTicks(
    isRead: Boolean,
    isDelivered: Boolean,
    modifier: Modifier = Modifier
) {
    if (isRead) {
        Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Read",
            tint = SmileGreenRead,
            modifier = modifier.size(16.dp)
        )
    } else if (isDelivered) {
        Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Delivered",
            tint = SmileMutedText,
            modifier = modifier.size(16.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Done,
            contentDescription = "Sent",
            tint = SmileMutedText,
            modifier = modifier.size(16.dp)
        )
    }
}

fun formatChatTimestamp(ts: Timestamp?): String {
    val date = ts?.toDate() ?: return ""
    val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return fmt.format(date)
}

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

@Composable
fun QrCodeCanvas(
    payload: String,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { MediaUtils.generateQrMatrix(payload) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(SmileWhite)
            .border(3.dp, SmileYellow, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.8f)) {
            val n = matrix.size
            val cell = this.size.width / n
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = SmileBlack,
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell * 0.92f, cell * 0.92f)
                        )
                    }
                }
            }
        }
    }
}
