package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PrivacyVisibility {
    EVERYONE, CONTACTS, NOBODY;
    companion object {
        fun fromRaw(raw: String?): PrivacyVisibility =
            entries.firstOrNull { it.name == raw } ?: EVERYONE
    }
}

enum class ChatType {
    DIRECT, GROUP;
    companion object {
        fun fromRaw(raw: String?): ChatType =
            entries.firstOrNull { it.name == raw } ?: DIRECT
    }
}

enum class MessageType {
    TEXT, IMAGE, DOCUMENT, VOICE, LOCATION, CONTACT, STICKER, SYSTEM;
    companion object {
        fun fromRaw(raw: String?): MessageType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class StatusType {
    TEXT, IMAGE;
    companion object {
        fun fromRaw(raw: String?): StatusType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class StatusPrivacy {
    EVERYONE, CONTACTS, EXCEPT;
    companion object {
        fun fromRaw(raw: String?): StatusPrivacy =
            entries.firstOrNull { it.name == raw } ?: CONTACTS
    }
}

enum class CallType {
    VOICE, VIDEO, GROUP_VOICE, GROUP_VIDEO;
    companion object {
        fun fromRaw(raw: String?): CallType =
            entries.firstOrNull { it.name == raw } ?: VOICE
    }
}

enum class CallStatus {
    RINGING, ACCEPTED, REJECTED, ENDED, MISSED;
    companion object {
        fun fromRaw(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: ENDED
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val twoStepPinHash: String = "",
    val appLockPinHash: String = "",
    val biometricLockEnabled: Boolean = false,
    val language: String = "en",
    val darkMode: Boolean = false,
    val wallpaperId: String = "default",
    val dataSaverEnabled: Boolean = false,
    val autoDownloadWifi: Boolean = true,
    val autoDownloadCellular: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val archivedChatIds: List<String> = emptyList(),
    val pinnedChatIds: List<String> = emptyList(),
    val mutedChatIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val smileId: String = "",
    val displayName: String = "",
    val about: String = "Hey there! I am using SMILE 😊",
    val phoneHash: String = "",
    val photoBase64: String = "",
    val ecdhPublicKey: String = "",
    val online: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    val photoVisibility: String = PrivacyVisibility.EVERYONE.name,
    val aboutVisibility: String = PrivacyVisibility.EVERYONE.name,
    val lastSeenVisibility: String = PrivacyVisibility.EVERYONE.name,
    val readReceiptsEnabled: Boolean = true,
    val publicDirectory: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ContactEntry(
    @DocumentId val id: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val smileId: String = "",
    val phoneHash: String = "",
    val about: String = "",
    val photoBase64: String = "",
    val ecdhPublicKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LookupIndex(
    @DocumentId val id: String = "",
    val userId: String = "",
    val smileId: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val chatType: String = ChatType.DIRECT.name,
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val coAdminUids: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val photoBase64: String = "",
    val inviteCode: String = "",
    val onlyAdminsCanSend: Boolean = false,
    val disappearingSeconds: Long = 0L,
    val communityId: String = "",
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageTime: Timestamp? = null,
    val typingUids: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = MessageType.TEXT.name,
    val ciphertext: String = "",
    val iv: String = "",
    val plainFallback: String = "",
    val mediaBase64: String = "",
    val fileName: String = "",
    val fileMimeType: String = "",
    val durationSec: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val sharedContactName: String = "",
    val sharedContactPhone: String = "",
    val replyToId: String = "",
    val replyToSnippet: String = "",
    val forwarded: Boolean = false,
    val edited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val deletedForUids: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    val starredByUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val deliveredToUids: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusUpdate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarBase64: String = "",
    val statusType: String = StatusType.TEXT.name,
    val textContent: String = "",
    val bgColorHex: String = "#FFD600",
    val caption: String = "",
    val mediaBase64: String = "",
    val privacy: String = StatusPrivacy.EVERYONE.name,
    val allowedUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StatusView(
    @DocumentId val id: String = "",
    val viewerId: String = "",
    val viewerName: String = "",
    val statusOwnerId: String = "",
    @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeIds: List<String> = emptyList(),
    val participantUids: List<String> = emptyList(),
    val chatId: String = "",
    val callType: String = CallType.VOICE.name,
    val status: String = CallStatus.RINGING.name,
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconBase64: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val linkedChatIds: List<String> = emptyList(),
    val announcementChatId: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUid: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val notifType: String = "SYSTEM",
    val refId: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
