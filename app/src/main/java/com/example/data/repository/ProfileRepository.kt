package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.R
import com.example.core.E2eeCryptoManager
import com.example.data.model.BlockedUser
import com.example.data.model.ContactEntry
import com.example.data.model.LookupIndex
import com.example.data.model.PrivacyVisibility
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileRepository(
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

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "userProfiles/$userId"
        emitAll(
            db.collection("userProfiles").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeDirectoryProfiles(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "userProfiles"
        emitAll(
            db.collection("userProfiles")
                .whereEqualTo("publicDirectory", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    fun observeContacts(userId: String): Flow<List<ContactEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ContactEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
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

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/blockedUsers"
        emitAll(
            db.collection("users").document(userId).collection("blockedUsers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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

    suspend fun registerOrUpdateProfile(
        rawPhone: String,
        displayName: String,
        rawSmileId: String,
        about: String,
        photoBase64: String
    ): Result<Unit> {
        val uidOrPlaceholder = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Display name is required" }

            val normalizedPhone = E2eeCryptoManager.normalizeIndianPhone(rawPhone)
            require(normalizedPhone.length in 10..16) { "Enter a valid mobile number (+91)" }
            val phoneHash = E2eeCryptoManager.hashPhoneE164(normalizedPhone)

            val cleanSmileId = rawSmileId.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
            require(cleanSmileId.length in 3..32) { "Smile ID must be 3-32 characters (letters, digits, _ or .)" }

            // Check duplicate phone or Smile ID
            val existingPhoneSnap = db.collection("phoneLookup").document(phoneHash).get().await()
            if (existingPhoneSnap.exists() && existingPhoneSnap.getString("userId") != uid) {
                return Result.failure(IllegalStateException("This mobile number is already registered with another SMILE account."))
            }
            val existingIdSnap = db.collection("smileIdLookup").document(cleanSmileId).get().await()
            if (existingIdSnap.exists() && existingIdSnap.getString("userId") != uid) {
                return Result.failure(IllegalStateException("Smile ID @$cleanSmileId is already taken."))
            }

            val pubKey = crypto.getPublicKeyBase64(uid)
            val existingAccount = db.collection("users").document(uid).get().await()
            val existingProfile = db.collection("userProfiles").document(uid).get().await()

            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            if (!existingAccount.exists()) {
                val accountMap = mapOf(
                    "userId" to uid,
                    "phoneNumber" to normalizedPhone,
                    "language" to "en",
                    "darkMode" to false,
                    "wallpaperId" to "default",
                    "dataSaverEnabled" to false,
                    "autoDownloadWifi" to true,
                    "autoDownloadCellular" to false,
                    "notificationsEnabled" to true,
                    "archivedChatIds" to emptyList<String>(),
                    "pinnedChatIds" to emptyList<String>(),
                    "mutedChatIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(userRef, accountMap)
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "phoneNumber" to normalizedPhone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val profileRef = db.collection("userProfiles").document(uid)
            val cleanAbout = about.trim().ifEmpty { "Hey there! I am using SMILE 😊" }.take(200)
            if (!existingProfile.exists()) {
                val profileMap = buildMap<String, Any> {
                    put("userId", uid)
                    put("smileId", cleanSmileId)
                    put("displayName", cleanName)
                    put("about", cleanAbout)
                    put("phoneHash", phoneHash)
                    if (photoBase64.isNotBlank()) put("photoBase64", photoBase64)
                    put("ecdhPublicKey", pubKey)
                    put("online", true)
                    put("lastSeen", FieldValue.serverTimestamp())
                    put("photoVisibility", PrivacyVisibility.EVERYONE.name)
                    put("aboutVisibility", PrivacyVisibility.EVERYONE.name)
                    put("lastSeenVisibility", PrivacyVisibility.EVERYONE.name)
                    put("readReceiptsEnabled", true)
                    put("publicDirectory", true)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(profileRef, profileMap)
            } else {
                val updateMap = buildMap<String, Any> {
                    put("smileId", cleanSmileId)
                    put("displayName", cleanName)
                    put("about", cleanAbout)
                    put("phoneHash", phoneHash)
                    if (photoBase64.isNotBlank()) put("photoBase64", photoBase64)
                    put("ecdhPublicKey", pubKey)
                    put("online", true)
                    put("lastSeen", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.update(profileRef, updateMap)
            }

            val lookupPayload = mapOf(
                "userId" to uid,
                "smileId" to cleanSmileId,
                "displayName" to cleanName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val phoneLookupRef = db.collection("phoneLookup").document(phoneHash)
            if (!existingPhoneSnap.exists()) {
                batch.set(phoneLookupRef, lookupPayload)
            } else {
                batch.update(phoneLookupRef, mapOf(
                    "smileId" to cleanSmileId,
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            val idLookupRef = db.collection("smileIdLookup").document(cleanSmileId)
            if (!existingIdSnap.exists()) {
                batch.set(idLookupRef, lookupPayload)
            } else {
                batch.update(idLookupRef, mapOf(
                    "displayName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "userProfiles/$uidOrPlaceholder")
            Result.failure(e)
        }
    }

    suspend fun findUserByPhoneOrSmileId(query: String): Result<UserProfile?> {
        return try {
            val uid = requireUserId()
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return Result.success(null)

            val digitsCount = trimmed.count { it.isDigit() }
            val targetUid: String? = if (digitsCount >= 10 && !trimmed.startsWith("@")) {
                val normalized = E2eeCryptoManager.normalizeIndianPhone(trimmed)
                val hash = E2eeCryptoManager.hashPhoneE164(normalized)
                val snap = db.collection("phoneLookup").document(hash).get().await()
                snap.toObject(LookupIndex::class.java)?.userId
            } else {
                val idKey = trimmed.removePrefix("@").lowercase()
                val snap = db.collection("smileIdLookup").document(idKey).get().await()
                snap.toObject(LookupIndex::class.java)?.userId
            }

            if (targetUid.isNullOrBlank() || targetUid == uid) {
                return Result.success(null)
            }
            val profileSnap = db.collection("userProfiles").document(targetUid).get().await()
            val profile = profileSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "phoneLookup_or_smileIdLookup")
            Result.failure(e)
        }
    }

    suspend fun saveContact(profile: UserProfile): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("users").document(currentUid)
                .collection("contacts").document(profile.userId)
            val existing = ref.get().await()
            if (!existing.exists()) {
                val data = buildMap<String, Any> {
                    put("contactUid", profile.userId)
                    put("displayName", profile.displayName)
                    put("smileId", profile.smileId)
                    put("phoneHash", profile.phoneHash)
                    put("about", profile.about)
                    if (profile.photoBase64.isNotBlank()) put("photoBase64", profile.photoBase64)
                    put("ecdhPublicKey", profile.ecdhPublicKey)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.set(data).await()
            } else {
                val data = buildMap<String, Any> {
                    put("displayName", profile.displayName)
                    put("smileId", profile.smileId)
                    put("phoneHash", profile.phoneHash)
                    put("about", profile.about)
                    if (profile.photoBase64.isNotBlank()) put("photoBase64", profile.photoBase64)
                    put("ecdhPublicKey", profile.ecdhPublicKey)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                ref.update(data).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/contacts/${profile.userId}")
            Result.failure(e)
        }
    }

    suspend fun syncPhoneContacts(context: Context): Result<Int> {
        return try {
            val currentUid = requireUserId()
            val phoneHashes = withContext(Dispatchers.IO) {
                val hashes = mutableSetOf<String>()
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && hashes.size < 150) {
                        val raw = cursor.getString(idx) ?: continue
                        val normalized = E2eeCryptoManager.normalizeIndianPhone(raw)
                        if (normalized.length >= 12) {
                            hashes.add(E2eeCryptoManager.hashPhoneE164(normalized))
                        }
                    }
                }
                hashes.toList()
            }

            var matchedCount = 0
            for (hash in phoneHashes) {
                val lookup = db.collection("phoneLookup").document(hash).get().await()
                val targetUid = lookup.getString("userId")
                if (!targetUid.isNullOrBlank() && targetUid != currentUid) {
                    val profileSnap = db.collection("userProfiles").document(targetUid).get().await()
                    val profile = profileSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (profile != null) {
                        saveContact(profile)
                        matchedCount++
                    }
                }
            }
            Result.success(matchedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "phoneLookup")
            Result.failure(e)
        }
    }

    suspend fun setOnlineStatus(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            db.collection("userProfiles").document(uid)
                .update(
                    mapOf(
                        "online" to online,
                        "lastSeen" to FieldValue.serverTimestamp(),
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

    suspend fun updateAccountFields(fields: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            val payload = fields.toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
            db.collection("users").document(currentUid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfilePrivacy(
        photoVisibility: PrivacyVisibility,
        aboutVisibility: PrivacyVisibility,
        lastSeenVisibility: PrivacyVisibility,
        readReceiptsEnabled: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            db.collection("userProfiles").document(currentUid).update(
                mapOf(
                    "photoVisibility" to photoVisibility.name,
                    "aboutVisibility" to aboutVisibility.name,
                    "lastSeenVisibility" to lastSeenVisibility.name,
                    "readReceiptsEnabled" to readReceiptsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "userProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUid: String, targetName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid)
                .collection("blockedUsers").document(targetUid)
                .set(
                    mapOf(
                        "blockedUid" to targetUid,
                        "displayName" to targetName.take(80).ifBlank { "SMILE User" },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/blockedUsers/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            db.collection("users").document(currentUid)
                .collection("blockedUsers").document(targetUid)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/blockedUsers/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetUserId: String,
        reason: String,
        details: String,
        reportType: String
    ): Result<Unit> {
        return try {
            val currentUid = requireUserId()
            val ref = db.collection("reports").document()
            ref.set(
                mapOf(
                    "reporterId" to currentUid,
                    "targetUserId" to targetUserId.take(128),
                    "reason" to reason.trim().ifBlank { "General Report" }.take(120),
                    "details" to details.trim().take(1000),
                    "reportType" to reportType,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountDataAndAuth(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        return try {
            val currentUid = requireUserId()
            val profile = db.collection("userProfiles").document(currentUid).get().await()
                .toObject(UserProfile::class.java)
            if (profile != null) {
                if (profile.phoneHash.isNotBlank()) {
                    db.collection("phoneLookup").document(profile.phoneHash).delete().await()
                }
                if (profile.smileId.isNotBlank()) {
                    db.collection("smileIdLookup").document(profile.smileId).delete().await()
                }
            }
            db.collection("userProfiles").document(currentUid).delete().await()
            db.collection("users").document(currentUid).delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
