package com.example.core

import android.content.Context
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption manager using ECDH (secp256r1) + HKDF-SHA256 + AES-256-GCM.
 * Private keys stay exclusively on-device in private SharedPreferences.
 */
class E2eeCryptoManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("smile_e2ee_keys", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun getOrCreateIdentityKeyPair(uid: String): Pair<String, PrivateKey> {
        val privKeyKey = "ecdh_priv_$uid"
        val pubKeyKey = "ecdh_pub_$uid"
        val existingPriv = prefs.getString(privKeyKey, null)
        val existingPub = prefs.getString(pubKeyKey, null)

        if (!existingPriv.isNullOrBlank() && !existingPub.isNullOrBlank()) {
            return try {
                val kf = KeyFactory.getInstance("EC")
                val privBytes = Base64.decode(existingPriv, Base64.NO_WRAP)
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                existingPub to priv
            } catch (_: Exception) {
                generateAndStoreKeyPair(uid)
            }
        }
        return generateAndStoreKeyPair(uid)
    }

    private fun generateAndStoreKeyPair(uid: String): Pair<String, PrivateKey> {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val kp = kpg.generateKeyPair()
        val pubBase64 = Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP)
        val privBase64 = Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP)
        prefs.edit()
            .putString("ecdh_pub_$uid", pubBase64)
            .putString("ecdh_priv_$uid", privBase64)
            .apply()
        return pubBase64 to kp.private
    }

    fun getPublicKeyBase64(uid: String): String = getOrCreateIdentityKeyPair(uid).first

    fun deriveSharedAesKey(myUid: String, peerPublicKeyBase64: String, chatId: String): SecretKey {
        val (_, myPriv) = getOrCreateIdentityKeyPair(myUid)
        val kf = KeyFactory.getInstance("EC")
        val peerBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val peerPub: PublicKey = kf.generatePublic(X509EncodedKeySpec(peerBytes))

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(myPriv)
        ka.doPhase(peerPub, true)
        val sharedSecret = ka.generateSecret()

        val info = "SMILE-E2EE-V1-$chatId".toByteArray(Charsets.UTF_8)
        val derived = hkdfSha256(sharedSecret, chatId.toByteArray(Charsets.UTF_8), info, 32)
        return SecretKeySpec(derived, "AES")
    }

    fun deriveGroupAesKey(chatId: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("SMILE-GROUP-KEY-V1:$chatId".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    fun encryptText(plaintext: String, key: SecretKey): Pair<String, String> {
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    fun decryptText(ciphertextBase64: String, ivBase64: String, key: SecretKey): String? {
        return try {
            val cipherBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun computeSafetyNumber(pubA: String, pubB: String): String {
        val ordered = listOf(pubA, pubB).sorted().joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256").digest(ordered.toByteArray(Charsets.UTF_8))
        val digits = StringBuilder()
        for (i in 0 until 20) {
            val v = ((hash[i].toInt() and 0xFF) * 100 + (hash[(i + 1) % 32].toInt() and 0xFF)) % 1000
            digits.append(v.toString().padStart(3, '0'))
            if ((i + 1) % 4 == 0 && i < 19) digits.append("  ")
        }
        return digits.toString()
    }

    private fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(1.toByte())
        return mac.doFinal().copyOf(length)
    }

    companion object {
        fun normalizeIndianPhone(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            return when {
                digits.length == 10 -> "+91$digits"
                digits.length == 12 && digits.startsWith("91") -> "+$digits"
                raw.trim().startsWith("+") -> "+" + digits
                digits.isNotEmpty() -> "+91$digits"
                else -> ""
            }
        }

        fun hashPhoneE164(normalizedPhone: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(normalizedPhone.trim().toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }

        fun hashPin(pin: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val saltHex = salt.joinToString("") { "%02x".format(it) }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$saltHex:$pin".toByteArray(Charsets.UTF_8))
            val hashHex = digest.joinToString("") { "%02x".format(it) }
            return "$saltHex:$hashHex"
        }

        fun verifyPin(pin: String, storedHash: String): Boolean {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            val saltHex = parts[0]
            val expectedHex = parts[1]
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$saltHex:$pin".toByteArray(Charsets.UTF_8))
            val actualHex = digest.joinToString("") { "%02x".format(it) }
            return MessageDigest.isEqual(actualHex.toByteArray(), expectedHex.toByteArray())
        }
    }
}
