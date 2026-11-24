package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.E2eeCryptoManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.ChatType
import com.example.data.model.MessageType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

class ChatRepository(
    private val db: FirebaseFirestore,
    private val crypto: E2eeCryptoManager,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        E2eeCryptoManager(context.applicationContext)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeChats(userId: String): Flow<List<ChatRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageTime ?: it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChat(chatId: String): Flow<ChatRoom?> = flow {
        val uid = requireUserId()
        val path = "chats/$chatId"
        emitAll(
            db.collection("chats").document(chatId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    val now = Date()
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { msg ->
                            val exp = msg.expiresAt?.toDate()
                            (exp == null || exp.after(now)) && !msg.deletedForUids.contains(userId)
                        }
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fe ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun resolveChatAesKey(chat: ChatRoom, myUid: String, peerPublicKeyBase64: String?): SecretKey {
        return if (chat.chatType == ChatType.DIRECT.name && !peerPublicKeyBase64.isNullOrBlank()) {
            try {
                crypto.deriveSharedAesKey(myUid, peerPublicKeyBase64, chat.id)
            } catch (_: Exception) {
                crypto.deriveGroupAesKey(chat.id)
            }
        } else {
            crypto.deriveGroupAesKey(chat.id)
        }
    }

    fun decryptMessageText(msg: ChatMessage, key: SecretKey): String {
        if (msg.deletedForEveryone) return "This message was deleted"
        if (msg.ciphertext.isNotBlank() && msg.iv.isNotBlank()) {
            crypto.decryptText(msg.ciphertext, msg.iv, key)?.let { return it }
        }
        return msg.plainFallback
    }

    suspend fun createOrGetDirectChat(peerUid: String): Result<String> {
        return try {
            val currentUid = requireUserId()
            val sorted = listOf(currentUid, peerUid).sorted()
            val chatId = "dm_${sorted[0]}_${sorted[1]}"
            val chatRef = db.collection("chats").document(chatId)
            val snap = chatRef.get().await()
            if (!snap.exists()) {
                chatRef.set(
                    mapOf(
                        "chatType" to ChatType.DIRECT.name,
                        "memberUids" to sorted,
                        "adminUids" to listOf(currentUid),
                        "coAdminUids" to emptyList<String>(),
                        "title" to "",
                        "description" to "",
                        "inviteCode" to "",
                        "onlyAdminsCanSend" to false,
                        "disappearingSeconds" to 0L,
                        "communityId" to "",
                        "lastMessageText" to "",
                        "lastMessageSenderId" to "",
                        "typingUids" to emptyList<String>(),
                        "unreadCounts" to mapOf(sorted[0] to 0L, sorted[1] to 0L),
                        "createdBy" to currentUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats")
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        photoBase64: String = "",
        communityId: String = "",
        onlyAdminsCanSend: Boolean = false
    ): Result<String> {
        return try {
            val currentUid = requireUserId()
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.isNotEmpty()) { "Group name is required" }
            val uniqueMembers = (listOf(currentUid) + memberUids).distinct().take(100)
            val inviteCode = "SMILE-${UUID.randomUUID().toString().take(8).uppercase()}"
            val chatRef = db.collection("chats").document()
            val unreadMap = uniqueMembers.associateWith { 0L }

            val batch = db.batch()
            val chatPayload = buildMap<String, Any> {
                put("chatType", ChatType.GROUP.name)
                put("memberUids", uniqueMembers)
                put("adminUids", listOf(currentUid))
                put("coAdminUids", emptyList<String>())
                put("title", cleanTitle)
                put("description", description.trim().take(300))
                if (photoBase64.isNotBlank()) put("photoBase64", photoBase64)
                put("inviteCode", inviteCode)
                put("onlyAdminsCanSend", onlyAdminsCanSend)
                put("disappearingSeconds", 0L)
                put("communityId", communityId)
                put("lastMessageText", "Group created")
                put("lastMessageSenderId", currentUid)
                put("lastMessageTime", FieldValue.serverTimestamp())
                put("typingUids", emptyList<String>())
                put("unreadCounts", unreadMap)
                put("createdBy", currentUid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.set(chatRef, chatPayload)

            val inviteRef = db.collection("groupInvites").document(inviteCode)
            batch.set(
                inviteRef,
                mapOf(
                    "chatId" to chatRef.id,
                    "title" to cleanTitle,
                    "description" to description.trim().take(300),
                    "createdBy" to currentUid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(chatRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats")
            Result.failure(e)
        }
    }

    suspend fun joinGroupByInviteCode(code: String): Result<String> {
        return try {
            val currentUid = requireUserId()
            val cleanCode = code.trim().uppercase()
            val inviteSnap = db.collection("groupInvites").document(cleanCode).get().await()
            val chatId = inviteSnap.getString("chatId")
                ?: return Result.failure(IllegalArgumentException("Invalid or expired invite code"))
            val chatRef = db.collection("chats").document(chatId)
            chatRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(currentUid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "groupInvites/$code")
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: ChatRoom,
        senderName: String,
        peerPublicKeyBase64: String?,
        messageType: MessageType,
        textBody: String,
        mediaBase64: String = "",
        fileName: String = "",
        fileMimeType: String = "",
        durationSec: Long = 0L,
        latitude: Double? = null,
        longitude: Double? = null,
        sharedContactName: String = "",
        sharedContactPhone: String = "",
        replyToId: String = "",
        replyToSnippet: String = "",
        forwarded: Boolean = false
    ): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val msgRef = db.collection("chats").document(chat.id).collection("messages").document()
            val key = resolveChatAesKey(chat, currentUid, peerPublicKeyBase64)
            val (ciphertext, iv) = if (textBody.isNotBlank()) {
                crypto.encryptText(textBody.take(2000), key)
            } else "" to ""

            val expiresAt = if (chat.disappearingSeconds > 0L) {
                Timestamp(Date(System.currentTimeMillis() + chat.disappearingSeconds * 1000L))
            } else null

            val previewLabel = when (messageType) {
                MessageType.TEXT -> "🔒 Encrypted message"
                MessageType.IMAGE -> "📷 Photo"
                MessageType.DOCUMENT -> "📄 ${fileName.ifBlank { "Document" }}"
                MessageType.VOICE -> "🎤 Voice note (${durationSec}s)"
                MessageType.LOCATION -> "📍 Shared Location"
                MessageType.CONTACT -> "👤 Contact: $sharedContactName"
                MessageType.STICKER -> textBody.take(16)
                MessageType.SYSTEM -> textBody.take(120)
            }

            val msgPayload = buildMap<String, Any> {
                put("senderId", currentUid)
                put("senderName", senderName.trim().ifBlank { "SMILE User" }.take(80))
                put("memberUids", chat.memberUids)
                put("messageType", messageType.name)
                put("ciphertext", ciphertext)
                put("iv", iv)
                put("plainFallback", "")
                if (mediaBase64.isNotBlank()) put("mediaBase64", mediaBase64)
                if (fileName.isNotBlank()) put("fileName", fileName.take(200))
                if (fileMimeType.isNotBlank()) put("fileMimeType", fileMimeType.take(100))
                if (durationSec > 0L) put("durationSec", durationSec)
                if (latitude != null) put("latitude", latitude)
                if (longitude != null) put("longitude", longitude)
                if (sharedContactName.isNotBlank()) put("sharedContactName", sharedContactName.take(80))
                if (sharedContactPhone.isNotBlank()) put("sharedContactPhone", sharedContactPhone.take(30))
                if (replyToId.isNotBlank()) put("replyToId", replyToId.take(128))
                if (replyToSnippet.isNotBlank()) put("replyToSnippet", replyToSnippet.take(200))
                put("forwarded", forwarded)
                put("edited", false)
                put("deletedForEveryone", false)
                put("deletedForUids", emptyList<String>())
                put("reactions", emptyMap<String, String>())
                put("starredByUids", emptyList<String>())
                put("readByUids", listOf(currentUid))
                put("deliveredToUids", listOf(currentUid))
                if (expiresAt != null) put("expiresAt", expiresAt)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val updatedUnread = chat.unreadCounts.toMutableMap()
            chat.memberUids.forEach { memberUid ->
                if (memberUid == currentUid) {
                    updatedUnread[memberUid] = 0L
                } else {
                    updatedUnread[memberUid] = (updatedUnread[memberUid] ?: 0L) + 1L
                }
            }

            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                db.collection("chats").document(chat.id),
                mapOf(
                    "lastMessageText" to previewLabel.take(300),
                    "lastMessageSenderId" to currentUid,
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "unreadCounts" to updatedUnread,
                    "typingUids" to FieldValue.arrayRemove(currentUid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "chats/${chat.id}/messages")
            Result.failure(e)
        }
    }

    suspend fun editMessage(
        chat: ChatRoom,
        messageId: String,
        newText: String,
        peerPublicKeyBase64: String?
    ): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val key = resolveChatAesKey(chat, currentUid, peerPublicKeyBase64)
            val (ciphertext, iv) = crypto.encryptText(newText.trim().take(2000), key)
            db.collection("chats").document(chat.id)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "ciphertext" to ciphertext,
                        "iv" to iv,
                        "edited" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/${chat.id}/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "ciphertext" to "",
                        "iv" to "",
                        "plainFallback" to "",
                        "deletedForEveryone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForMe(chatId: String, messageId: String): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deletedForUids" to FieldValue.arrayUnion(currentUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val updated = message.reactions.toMutableMap()
            if (updated[currentUid] == emoji) updated.remove(currentUid) else updated[currentUid] = emoji
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "reactions" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleStarMessage(chatId: String, message: ChatMessage): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val op = if (message.starredByUids.contains(currentUid)) {
                FieldValue.arrayRemove(currentUid)
            } else {
                FieldValue.arrayUnion(currentUid)
            }
            db.collection("chats").document(chatId)
                .collection("messages").document(message.id)
                .update(
                    mapOf(
                        "starredByUids" to op,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
            Result.failure(e)
        }
    }

    suspend fun markChatAsRead(chat: ChatRoom, messages: List<ChatMessage>): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val batch = db.batch()
            var hasUpdates = false
            if ((chat.unreadCounts[currentUid] ?: 0L) > 0L) {
                val updatedUnread = chat.unreadCounts.toMutableMap().apply { put(currentUid, 0L) }
                batch.update(
                    db.collection("chats").document(chat.id),
                    mapOf(
                        "unreadCounts" to updatedUnread,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                hasUpdates = true
            }
            messages.filter { !it.readByUids.contains(currentUid) }.take(25).forEach { msg ->
                val ref = db.collection("chats").document(chat.id)
                    .collection("messages").document(msg.id)
                batch.update(
                    ref,
                    mapOf(
                        "readByUids" to FieldValue.arrayUnion(currentUid),
                        "deliveredToUids" to FieldValue.arrayUnion(currentUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                hasUpdates = true
            }
            if (hasUpdates) batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            db.collection("chats").document(chatId)
                .update(
                    mapOf(
                        "typingUids" to if (isTyping) FieldValue.arrayUnion(currentUid) else FieldValue.arrayRemove(currentUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDisappearingTimer(chatId: String, seconds: Long): Result<Unit> {
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .update(
                    mapOf(
                        "disappearingSeconds" to seconds.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }

    suspend fun updateGroupManagement(
        chatId: String,
        title: String,
        description: String,
        onlyAdminsCanSend: Boolean,
        memberUids: List<String>,
        coAdminUids: List<String>
    ): Result<Unit> {
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .update(
                    mapOf(
                        "title" to title.trim().take(80),
                        "description" to description.trim().take(300),
                        "onlyAdminsCanSend" to onlyAdminsCanSend,
                        "memberUids" to memberUids.distinct().take(100),
                        "coAdminUids" to coAdminUids.distinct().take(20),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
            Result.failure(e)
        }
    }
}
