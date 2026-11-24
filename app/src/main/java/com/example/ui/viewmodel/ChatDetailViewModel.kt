package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.E2eeCryptoManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.MessageType
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatDetailViewModel(
    private val chatRepo: ChatRepository,
    private val crypto: E2eeCryptoManager,
    val chatId: String,
    val currentUserId: String
) : ViewModel() {

    val chatState: StateFlow<UiState<ChatRoom?>> = chatRepo.observeChat(chatId)
        .map<ChatRoom?, UiState<ChatRoom?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing chat", root)
            emit(UiState.Error(root.message ?: "Failed to load chat"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = chatRepo.observeMessages(chatId, currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing messages", root)
            emit(UiState.Error(root.message ?: "Failed to load messages"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _replyTarget = MutableStateFlow<Pair<String, String>?>(null)
    val replyTarget: StateFlow<Pair<String, String>?> = _replyTarget.asStateFlow()

    private val _editingTarget = MutableStateFlow<Pair<String, String>?>(null)
    val editingTarget: StateFlow<Pair<String, String>?> = _editingTarget.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    fun setReplyTarget(messageId: String, snippet: String) {
        _editingTarget.value = null
        _replyTarget.value = messageId to snippet
    }

    fun clearReplyTarget() {
        _replyTarget.value = null
    }

    fun setEditingTarget(messageId: String, currentText: String) {
        _replyTarget.value = null
        _editingTarget.value = messageId to currentText
    }

    fun clearEditingTarget() {
        _editingTarget.value = null
    }

    fun clearErrorBanner() {
        _errorBanner.value = null
    }

    fun decryptMessage(chat: ChatRoom, msg: ChatMessage, peerPubKey: String?): String {
        val key = chatRepo.resolveChatAesKey(chat, currentUserId, peerPubKey)
        return chatRepo.decryptMessageText(msg, key)
    }

    fun computeSafetyNumber(peerPubKey: String?): String {
        if (peerPubKey.isNullOrBlank()) return "Group symmetric key active"
        val myPub = crypto.getPublicKeyBase64(currentUserId)
        return crypto.computeSafetyNumber(myPub, peerPubKey)
    }

    fun sendOrEditTextMessage(
        chat: ChatRoom,
        senderName: String,
        peerPubKey: String?,
        text: String
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val editing = _editingTarget.value
        if (editing != null) {
            _editingTarget.value = null
            viewModelScope.launch {
                chatRepo.editMessage(chat, editing.first, trimmed, peerPubKey).onFailure {
                    _errorBanner.value = it.localizedMessage ?: "Edit failed"
                }
            }
            return
        }
        val reply = _replyTarget.value
        _replyTarget.value = null
        viewModelScope.launch {
            chatRepo.sendMessage(
                chat = chat,
                senderName = senderName,
                peerPublicKeyBase64 = peerPubKey,
                messageType = MessageType.TEXT,
                textBody = trimmed,
                replyToId = reply?.first ?: "",
                replyToSnippet = reply?.second ?: ""
            ).onFailure {
                _errorBanner.value = it.localizedMessage ?: "Message failed to send"
            }
        }
    }

    fun sendMediaOrRichMessage(
        chat: ChatRoom,
        senderName: String,
        peerPubKey: String?,
        messageType: MessageType,
        textBody: String = "",
        mediaBase64: String = "",
        fileName: String = "",
        fileMimeType: String = "",
        durationSec: Long = 0L,
        latitude: Double? = null,
        longitude: Double? = null,
        sharedContactName: String = "",
        sharedContactPhone: String = "",
        forwarded: Boolean = false
    ) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                chat = chat,
                senderName = senderName,
                peerPublicKeyBase64 = peerPubKey,
                messageType = messageType,
                textBody = textBody,
                mediaBase64 = mediaBase64,
                fileName = fileName,
                fileMimeType = fileMimeType,
                durationSec = durationSec,
                latitude = latitude,
                longitude = longitude,
                sharedContactName = sharedContactName,
                sharedContactPhone = sharedContactPhone,
                forwarded = forwarded
            ).onFailure {
                _errorBanner.value = it.localizedMessage ?: "Attachment failed to send"
            }
        }
    }

    fun markAsRead(chat: ChatRoom, messages: List<ChatMessage>) {
        viewModelScope.launch {
            chatRepo.markChatAsRead(chat, messages)
        }
    }

    fun setTyping(isTyping: Boolean) {
        viewModelScope.launch {
            chatRepo.setTyping(chatId, isTyping)
        }
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            chatRepo.toggleReaction(chatId, message, emoji)
        }
    }

    fun toggleStar(message: ChatMessage) {
        viewModelScope.launch {
            chatRepo.toggleStarMessage(chatId, message)
        }
    }

    fun deleteForMe(messageId: String) {
        viewModelScope.launch {
            chatRepo.deleteMessageForMe(chatId, messageId)
        }
    }

    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch {
            chatRepo.deleteMessageForEveryone(chatId, messageId)
        }
    }

    fun setDisappearingTimer(seconds: Long) {
        viewModelScope.launch {
            chatRepo.setDisappearingTimer(chatId, seconds)
        }
    }

    fun updateGroupSettings(
        title: String,
        description: String,
        onlyAdminsCanSend: Boolean,
        memberUids: List<String>,
        coAdminUids: List<String>
    ) {
        viewModelScope.launch {
            chatRepo.updateGroupManagement(chatId, title, description, onlyAdminsCanSend, memberUids, coAdminUids)
                .onFailure { _errorBanner.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun exportChatText(chat: ChatRoom, messages: List<ChatMessage>, peerPubKey: String?): String {
        val sb = StringBuilder()
        sb.appendLine("SMILE Encrypted Chat Export - ${chat.title.ifBlank { "Direct Chat" }}")
        sb.appendLine("======================================")
        messages.forEach { msg ->
            val body = decryptMessage(chat, msg, peerPubKey)
            val time = msg.createdAt?.toDate()?.toString() ?: ""
            sb.appendLine("[$time] ${msg.senderName}: $body")
        }
        return sb.toString()
    }

    private companion object {
        const val TAG = "ChatDetailVM"
    }
}
