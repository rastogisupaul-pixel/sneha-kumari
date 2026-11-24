package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.Community
import com.example.data.model.StatusPrivacy
import com.example.data.model.StatusType
import com.example.data.model.StatusUpdate
import com.example.data.model.StatusView
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

class SocialAndCallRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- STATUS / UPDATES ---

    fun observeStatuses(userId: String): Flow<List<StatusUpdate>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "statuses"
        emitAll(
            db.collection("statuses")
                .whereArrayContains("allowedUids", userId)
                .snapshots()
                .map { snap ->
                    val now = Date()
                    snap.toObjects(StatusUpdate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { st ->
                            val exp = st.expiresAt?.toDate()
                            (exp == null || exp.after(now)) && !st.mutedByUids.contains(userId)
                        }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun postStatus(
        userName: String,
        userAvatarBase64: String,
        statusType: StatusType,
        textContent: String,
        bgColorHex: String,
        caption: String,
        mediaBase64: String,
        privacy: StatusPrivacy,
        allowedContactUids: List<String>
    ): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val allowed = (listOf(currentUid) + allowedContactUids).distinct().take(200)
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
            val ref = db.collection("statuses").document()
            val payload = buildMap<String, Any> {
                put("userId", currentUid)
                put("userName", userName.trim().ifBlank { "SMILE User" }.take(80))
                if (userAvatarBase64.isNotBlank()) put("userAvatarBase64", userAvatarBase64)
                put("statusType", statusType.name)
                put("textContent", textContent.trim().take(500))
                put("bgColorHex", bgColorHex.take(16))
                put("caption", caption.trim().take(200))
                if (mediaBase64.isNotBlank()) put("mediaBase64", mediaBase64)
                put("privacy", privacy.name)
                put("allowedUids", allowed)
                put("mutedByUids", emptyList<String>())
                put("expiresAt", expiresAt)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "statuses")
            Result.failure(e)
        }
    }

    suspend fun recordStatusView(statusId: String, statusOwnerId: String, viewerName: String): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            if (currentUid == statusOwnerId) return Result.success(Unit)
            val ref = db.collection("statuses").document(statusId)
                .collection("views").document(currentUid)
            ref.set(
                mapOf(
                    "viewerId" to currentUid,
                    "viewerName" to viewerName.trim().ifBlank { "SMILE User" }.take(80),
                    "statusOwnerId" to statusOwnerId,
                    "viewedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeStatusViews(statusId: String, ownerUid: String): Flow<List<StatusView>> = flow {
        val uid = requireUserId()
        val path = "statuses/$statusId/views"
        emitAll(
            db.collection("statuses").document(statusId).collection("views")
                .whereEqualTo("statusOwnerId", ownerUid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StatusView::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun muteStatus(statusId: String): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            db.collection("statuses").document(statusId)
                .update(
                    mapOf(
                        "mutedByUids" to FieldValue.arrayUnion(currentUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "statuses/$statusId")
            Result.failure(e)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "statuses/$statusId")
            Result.failure(e)
        }
    }

    // --- CALLS (WEBRTC SIGNALING & HISTORY) ---

    fun observeCalls(userId: String): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun startCall(
        callerName: String,
        calleeIds: List<String>,
        chatId: String,
        callType: CallType
    ): Result<String> {
        return try {
            val currentUid = requireUserId()
            val cleanCallees = calleeIds.filter { it != currentUid && it.isNotBlank() }.distinct().take(30)
            require(cleanCallees.isNotEmpty()) { "At least one recipient required for call" }
            val participants = (listOf(currentUid) + cleanCallees).distinct()
            val ref = db.collection("calls").document()
            ref.set(
                mapOf(
                    "callerId" to currentUid,
                    "callerName" to callerName.trim().ifBlank { "SMILE User" }.take(80),
                    "calleeIds" to cleanCallees,
                    "participantUids" to participants,
                    "chatId" to chatId.take(128),
                    "callType" to callType.name,
                    "status" to CallStatus.RINGING.name,
                    "sdpOffer" to "v=0\no=smile-webrtc 1 2 IN IP4 127.0.0.1\ns=SMILE Call\n",
                    "durationSeconds" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "calls")
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(
        callId: String,
        status: CallStatus,
        durationSeconds: Long = 0L,
        sdpAnswer: String? = null
    ): Result<Unit> {
        return try {
            requireUserId()
            val payload = buildMap<String, Any> {
                put("status", status.name)
                put("durationSeconds", durationSeconds.coerceAtLeast(0L))
                if (!sdpAnswer.isNullOrBlank()) put("sdpAnswer", sdpAnswer)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("calls").document(callId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
            Result.failure(e)
        }
    }

    // --- COMMUNITIES ---

    fun observeCommunities(userId: String): Flow<List<Community>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "communities"
        emitAll(
            db.collection("communities")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
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

    suspend fun createCommunity(
        name: String,
        description: String,
        memberUids: List<String>,
        chatRepository: ChatRepository
    ): Result<String> {
        return try {
            val currentUid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Community name is required" }
            val uniqueMembers = (listOf(currentUid) + memberUids).distinct().take(200)

            // Create the announcement group automatically
            val announcementChatRes = chatRepository.createGroupChat(
                title = "$cleanName Announcements",
                description = "Official community announcements",
                memberUids = uniqueMembers,
                onlyAdminsCanSend = true
            )
            val announcementChatId = announcementChatRes.getOrElse { return Result.failure(it) }

            val commRef = db.collection("communities").document()
            commRef.set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(300),
                    "adminUids" to listOf(currentUid),
                    "memberUids" to uniqueMembers,
                    "linkedChatIds" to listOf(announcementChatId),
                    "announcementChatId" to announcementChatId,
                    "createdBy" to currentUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(commRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communities")
            Result.failure(e)
        }
    }

    suspend fun linkGroupToCommunity(community: Community, chatId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("communities").document(community.id)
                .update(
                    mapOf(
                        "linkedChatIds" to FieldValue.arrayUnion(chatId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/${community.id}")
            Result.failure(e)
        }
    }
}
