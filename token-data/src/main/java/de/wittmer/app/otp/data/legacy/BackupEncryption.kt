package de.wittmer.app.otp.data.legacy

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class BackupPayloadType(val wireName: String) {
    JSON("json"),
    KEY_URI("key-uri");

    companion object {
        fun fromWire(value: String): BackupPayloadType? =
            entries.firstOrNull { it.wireName == value }
    }
}

class BackupEncryptionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

data class DecryptedBackup(
    val payloadType: BackupPayloadType,
    val plaintext: ByteArray
)

/**
 * Password-based AES-256-GCM container for portable OTP backups.
 * Device-bound keys are intentionally not used so restores work on another phone.
 */
object BackupEncryption {
    const val FORMAT = "netaigis-otp-encrypted"
    const val VERSION = 1
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 310_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128

    private val gson = Gson()
    private val secureRandom = SecureRandom()

    fun isEncryptedContainer(text: String): Boolean {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("{")) {
            return false
        }
        return try {
            val envelope = gson.fromJson(trimmed, EncryptedBackupEnvelope::class.java)
            envelope?.format == FORMAT
        } catch (_: JsonSyntaxException) {
            false
        }
    }

    fun encryptUtf8(plaintext: String, password: CharArray, payloadType: BackupPayloadType): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { secureRandom.nextBytes(it) }
        val keyBytes = deriveKeyBytes(password, salt, ITERATIONS)
        try {
            val key = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val envelope = EncryptedBackupEnvelope(
                format = FORMAT,
                version = VERSION,
                payload = payloadType.wireName,
                kdf = KDF,
                iterations = ITERATIONS,
                salt = Base64.encodeToString(salt, Base64.NO_WRAP),
                nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            )
            return gson.toJson(envelope)
        } catch (e: GeneralSecurityException) {
            throw BackupEncryptionException("Encryption failed", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decryptUtf8(containerJson: String, password: CharArray): Pair<BackupPayloadType, String> {
        require(password.isNotEmpty()) { "Password must not be empty" }
        val envelope = try {
            gson.fromJson(containerJson.trim(), EncryptedBackupEnvelope::class.java)
        } catch (e: JsonSyntaxException) {
            throw BackupEncryptionException("Not a valid encrypted backup", e)
        } ?: throw BackupEncryptionException("Not a valid encrypted backup")

        if (envelope.format != FORMAT) {
            throw BackupEncryptionException("Unsupported backup format")
        }
        if (envelope.version != VERSION) {
            throw BackupEncryptionException("Unsupported backup version: ${envelope.version}")
        }
        if (envelope.kdf != KDF) {
            throw BackupEncryptionException("Unsupported KDF: ${envelope.kdf}")
        }
        val payloadType = BackupPayloadType.fromWire(envelope.payload)
            ?: throw BackupEncryptionException("Unsupported payload type: ${envelope.payload}")
        if (envelope.iterations < 100_000) {
            throw BackupEncryptionException("KDF iterations too low")
        }

        val salt = decodeBase64(envelope.salt, "salt")
        val nonce = decodeBase64(envelope.nonce, "nonce")
        val ciphertext = decodeBase64(envelope.ciphertext, "ciphertext")
        if (nonce.size != NONCE_BYTES) {
            throw BackupEncryptionException("Invalid nonce length")
        }

        val keyBytes = deriveKeyBytes(password, salt, envelope.iterations)
        try {
            val key = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            val plaintext = cipher.doFinal(ciphertext)
            return payloadType to String(plaintext, StandardCharsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw BackupEncryptionException("Wrong password or corrupted file", e)
        } catch (e: GeneralSecurityException) {
            throw BackupEncryptionException("Decryption failed", e)
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveKeyBytes(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val factory = SecretKeyFactory.getInstance(KDF)
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decodeBase64(value: String, field: String): ByteArray {
        return try {
            Base64.decode(value, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            throw BackupEncryptionException("Invalid Base64 in $field", e)
        }
    }
}

private data class EncryptedBackupEnvelope(
    val format: String,
    val version: Int,
    val payload: String,
    val kdf: String,
    val iterations: Int,
    val salt: String,
    val nonce: String,
    val ciphertext: String
)

fun CharArray.wipe() {
    fill('\u0000')
}
