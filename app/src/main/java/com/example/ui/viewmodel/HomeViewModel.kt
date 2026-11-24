package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatRoom
import com.example.data.model.Community
import com.example.data.model.StatusPrivacy
import com.example.data.model.StatusType
import com.example.data.model.StatusUpdate
import com.example.data.repository.ChatRepository
import com.example.data.repository.SocialAndCallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatRepo: ChatRepository,
    private val socialRepo: SocialAndCallRepository,
    val currentUserId: String
) : ViewModel() {

    val chatsState: StateFlow<UiState<List<ChatRoom>>> = chatRepo.observeChats(currentUserId)
        .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing chats", root)
            emit(UiState.Error(root.message ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val statusesState: StateFlow<UiState<List<StatusUpdate>>> = socialRepo.observeStatuses(currentUserId)
        .map<List<StatusUpdate>, UiState<List<StatusUpdate>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing statuses", root)
            emit(UiState.Error(root.message ?: "Failed to load status updates"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val callsState: StateFlow<UiState<List<CallSession>>> = socialRepo.observeCalls(currentUserId)
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing calls", root)
            emit(UiState.Error(root.message ?: "Failed to load calls"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val communitiesState: StateFlow<UiState<List<Community>>> = socialRepo.observeCommunities(currentUserId)
        .map<List<Community>, UiState<List<Community>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing communities", root)
            emit(UiState.Error(root.message ?: "Failed to load communities"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _viewedStatusIds = MutableStateFlow<Set<String>>(emptySet())
    val viewedStatusIds: StateFlow<Set<String>> = _viewedStatusIds.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun startDirectChat(peerUid: String, onOpenChat: (String) -> Unit) {
        viewModelScope.launch {
            chatRepo.createOrGetDirectChat(peerUid).onSuccess { chatId ->
                onOpenChat(chatId)
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Unable to open chat"
            }
        }
    }

    fun createGroup(
        title: String,
        description: String,
        memberUids: List<String>,
        photoBase64: String,
        communityId: String = "",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            chatRepo.createGroupChat(title, description, memberUids, photoBase64, communityId)
                .onSuccess { chatId ->
                    _feedbackMessage.value = "Group \"$title\" created!"
                    onCreated(chatId)
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to create group"
                }
        }
    }

    fun joinGroupByCode(code: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            chatRepo.joinGroupByInviteCode(code)
                .onSuccess { chatId ->
                    _feedbackMessage.value = "Joined group!"
                    onJoined(chatId)
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Invalid invite code"
                }
        }
    }

    fun postStatus(
        userName: String,
        avatarBase64: String,
        statusType: StatusType,
        textContent: String,
        bgColorHex: String,
        caption: String,
        mediaBase64: String,
        privacy: StatusPrivacy,
        allowedUids: List<String>
    ) {
        viewModelScope.launch {
            socialRepo.postStatus(
                userName, avatarBase64, statusType, textContent,
                bgColorHex, caption, mediaBase64, privacy, allowedUids
            ).onSuccess {
                _feedbackMessage.value = "Status published for 24 hours!"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage ?: "Failed to post status"
            }
        }
    }

    fun markStatusViewed(status: StatusUpdate, viewerName: String) {
        _viewedStatusIds.value = _viewedStatusIds.value + status.id
        viewModelScope.launch {
            socialRepo.recordStatusView(status.id, status.userId, viewerName)
        }
    }

    fun muteStatus(statusId: String) {
        viewModelScope.launch {
            socialRepo.muteStatus(statusId)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            socialRepo.deleteStatus(statusId)
        }
    }

    fun startCall(
        callerName: String,
        calleeIds: List<String>,
        chatId: String,
        callType: CallType,
        onStarted: (String) -> Unit
    ) {
        viewModelScope.launch {
            socialRepo.startCall(callerName, calleeIds, chatId, callType)
                .onSuccess { callId -> onStarted(callId) }
                .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Unable to start call" }
        }
    }

    fun updateCallStatus(callId: String, status: CallStatus, durationSeconds: Long = 0L) {
        viewModelScope.launch {
            val answer = if (status == CallStatus.ACCEPTED) "v=0\no=smile-answer 1 2 IN IP4 127.0.0.1\n" else null
            socialRepo.updateCallStatus(callId, status, durationSeconds, answer)
        }
    }

    fun createCommunity(
        name: String,
        description: String,
        memberUids: List<String>,
        onCreated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            socialRepo.createCommunity(name, description, memberUids, chatRepo)
                .onSuccess {
                    _feedbackMessage.value = "Community \"$name\" created!"
                    onCreated()
                }
                .onFailure {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to create community"
                }
        }
    }

    fun linkGroupToCommunity(community: Community, chatId: String) {
        viewModelScope.launch {
            socialRepo.linkGroupToCommunity(community, chatId).onSuccess {
                _feedbackMessage.value = "Group linked to ${community.name}"
            }
        }
    }

    private companion object {
        const val TAG = "HomeVM"
    }
}
