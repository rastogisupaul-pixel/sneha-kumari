package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.E2eeCryptoManager
import com.example.data.model.BlockedUser
import com.example.data.model.ContactEntry
import com.example.data.model.PrivacyVisibility
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val profileRepo: ProfileRepository,
    val currentUserId: String
) : ViewModel() {

    val accountState: StateFlow<UiState<UserAccount?>> = profileRepo.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing user account", root)
            emit(UiState.Error(root.message ?: "Failed to load account"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myProfileState: StateFlow<UiState<UserProfile?>> = profileRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing user profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val contacts: StateFlow<List<ContactEntry>> = profileRepo.observeContacts(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing contacts", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val directoryProfiles: StateFlow<List<UserProfile>> = profileRepo.observeDirectoryProfiles()
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing directory", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedUsers: StateFlow<List<BlockedUser>> = profileRepo.observeBlockedUsers(currentUserId)
        .catch { error ->
            val root = unwrapFirestoreStreamError(error)
            Log.w(TAG, "Error observing blocked users", root)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _searchResultProfile = MutableStateFlow<UserProfile?>(null)
    val searchResultProfile: StateFlow<UserProfile?> = _searchResultProfile.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepo.setOnlineStatus(true)
        }
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
    }

    fun verifyPinAndUnlock(pin: String, storedHash: String): Boolean {
        val ok = E2eeCryptoManager.verifyPin(pin, storedHash)
        if (ok) _isAppUnlocked.value = true
        return ok
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearSearchResult() {
        _searchResultProfile.value = null
    }

    fun saveProfileSetup(
        rawPhone: String,
        displayName: String,
        smileId: String,
        about: String,
        photoBase64: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.registerOrUpdateProfile(rawPhone, displayName, smileId, about, photoBase64)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "SMILE profile saved!"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun searchUser(query: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.findUserByPhoneOrSmileId(query)
            _isBusy.value = false
            res.onSuccess { profile ->
                _searchResultProfile.value = profile
                if (profile == null) {
                    _actionMessage.value = "No SMILE user found for \"$query\". Invite them!"
                }
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Search failed"
            }
        }
    }

    fun addContact(profile: UserProfile) {
        viewModelScope.launch {
            profileRepo.saveContact(profile)
        }
    }

    fun syncPhoneContacts(context: Context) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.syncPhoneContacts(context)
            _isBusy.value = false
            res.onSuccess { count ->
                _actionMessage.value = "Synced $count SMILE contact(s)!"
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Contact sync failed"
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            profileRepo.updateAccountFields(mapOf("darkMode" to enabled))
        }
    }

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            profileRepo.updateAccountFields(mapOf("language" to langCode))
        }
    }

    fun setWallpaper(wallpaperId: String) {
        viewModelScope.launch {
            profileRepo.updateAccountFields(mapOf("wallpaperId" to wallpaperId))
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepo.updateAccountFields(mapOf("notificationsEnabled" to enabled))
        }
    }

    fun setDataUsageSettings(dataSaver: Boolean, wifiAuto: Boolean, cellAuto: Boolean) {
        viewModelScope.launch {
            profileRepo.updateAccountFields(
                mapOf(
                    "dataSaverEnabled" to dataSaver,
                    "autoDownloadWifi" to wifiAuto,
                    "autoDownloadCellular" to cellAuto
                )
            )
        }
    }

    fun setAppLockPin(pin: String, biometricEnabled: Boolean) {
        viewModelScope.launch {
            val hash = if (pin.isBlank()) "" else E2eeCryptoManager.hashPin(pin)
            profileRepo.updateAccountFields(
                mapOf(
                    "appLockPinHash" to hash,
                    "biometricLockEnabled" to biometricEnabled
                )
            )
            _actionMessage.value = if (hash.isBlank()) "App lock disabled" else "App lock updated"
        }
    }

    fun setTwoStepPin(pin: String) {
        viewModelScope.launch {
            val hash = if (pin.isBlank()) "" else E2eeCryptoManager.hashPin(pin)
            profileRepo.updateAccountFields(mapOf("twoStepPinHash" to hash))
            _actionMessage.value = if (hash.isBlank()) "Two-step verification turned off" else "Two-step PIN saved"
        }
    }

    fun togglePinnedChat(chatId: String, currentPinned: List<String>) {
        viewModelScope.launch {
            val updated = if (currentPinned.contains(chatId)) currentPinned - chatId else (listOf(chatId) + currentPinned).take(50)
            profileRepo.updateAccountFields(mapOf("pinnedChatIds" to updated))
        }
    }

    fun toggleArchivedChat(chatId: String, currentArchived: List<String>) {
        viewModelScope.launch {
            val updated = if (currentArchived.contains(chatId)) currentArchived - chatId else (currentArchived + chatId).take(200)
            profileRepo.updateAccountFields(mapOf("archivedChatIds" to updated))
        }
    }

    fun toggleMutedChat(chatId: String, currentMuted: List<String>) {
        viewModelScope.launch {
            val updated = if (currentMuted.contains(chatId)) currentMuted - chatId else (currentMuted + chatId).take(200)
            profileRepo.updateAccountFields(mapOf("mutedChatIds" to updated))
        }
    }

    fun updatePrivacy(
        photoVis: PrivacyVisibility,
        aboutVis: PrivacyVisibility,
        lastSeenVis: PrivacyVisibility,
        readReceipts: Boolean
    ) {
        viewModelScope.launch {
            profileRepo.updateProfilePrivacy(photoVis, aboutVis, lastSeenVis, readReceipts)
            _actionMessage.value = "Privacy settings updated"
        }
    }

    fun blockUser(targetUid: String, displayName: String) {
        viewModelScope.launch {
            profileRepo.blockUser(targetUid, displayName).onSuccess {
                _actionMessage.value = "$displayName blocked"
            }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            profileRepo.unblockUser(targetUid).onSuccess {
                _actionMessage.value = "User unblocked"
            }
        }
    }

    fun submitReport(targetUid: String, reason: String, details: String, reportType: String) {
        viewModelScope.launch {
            profileRepo.submitReport(targetUid, reason, details, reportType).onSuccess {
                _actionMessage.value = "Report submitted securely. Thank you."
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun deleteMyAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.deleteAccountDataAndAuth()
            _isBusy.value = false
            res.onSuccess {
                onDeleted()
            }.onFailure {
                _actionMessage.value = it.localizedMessage ?: "Re-authenticate recently to delete account."
            }
        }
    }

    fun setOfflineOnDispose() {
        viewModelScope.launch {
            profileRepo.setOnlineStatus(false)
        }
    }

    private companion object {
        const val TAG = "SessionVM"
    }
}
