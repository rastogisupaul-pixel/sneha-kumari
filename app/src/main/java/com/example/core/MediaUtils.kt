package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

object MediaUtils {

    fun compressImageUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 72
    ): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val scale = min(
                maxDimension.toFloat() / original.width.coerceAtLeast(1),
                maxDimension.toFloat() / original.height.coerceAtLeast(1)
            ).coerceAtMost(1f)

            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (targetW != original.width || targetH != original.height) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (out.size() > 140_000) {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, 48, out)
            }
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun readDocumentInfo(context: Context, uri: Uri): Triple<String, String, String?> {
        var name = "Document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: "Document"
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val encoded = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.size <= 220_000) {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else null
            }
        } catch (_: Exception) {
            null
        }
        return Triple(name, mime, encoded)
    }

    fun generateQrMatrix(payload: String, size: Int = 21): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) }
        fun drawFinder(top: Int, left: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[top + r][left + c] = isBorder || isCenter
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val byteVal = digest[(bitIndex / 8) % digest.size].toInt()
                    val bit = (byteVal shr (bitIndex % 8)) and 1
                    matrix[r][c] = bit == 1 || ((r + c) % 7 == 0)
                    bitIndex++
                }
            }
        }
        return matrix
    }
}

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(16000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            startTimeMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            cleanup()
            false
        }
    }

    fun stopAndEncode(): Pair<String, Int>? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceAtLeast(1)
            val file = outputFile ?: return null
            val bytes = file.readBytes()
            file.delete()
            if (bytes.size > 240_000) return null
            Base64.encodeToString(bytes, Base64.NO_WRAP) to durationSec
        } catch (_: Exception) {
            cleanup()
            null
        }
    }

    fun cleanup() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}

class VoiceNotePlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playBase64(base64: String, onCompletion: () -> Unit) {
        stop()
        try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val temp = File(context.cacheDir, "play_${System.currentTimeMillis()}.m4a")
            FileOutputStream(temp).use { it.write(bytes) }
            player = MediaPlayer().apply {
                setDataSource(temp.absolutePath)
                prepare()
                setOnCompletionListener {
                    temp.delete()
                    onCompletion()
                }
                start()
            }
        } catch (_: Exception) {
            onCompletion()
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
