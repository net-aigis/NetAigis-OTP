package de.wittmer.app.otp.data.legacy

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import de.wittmer.app.otp.data.MigrationUtil
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.data.OtpTokenFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportExportUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val migrationUtil: MigrationUtil,
    private val gson: Gson,
    private val otpTokenDatabase: OtpTokenDatabase
) {
    suspend fun importJsonFile(uri: Uri, password: CharArray? = null) {
        withContext(Dispatchers.IO) {
            val raw = readUtf8(uri)
            val jsonText = resolvePayload(raw, password, BackupPayloadType.JSON)
            val savedTokens = gson.fromJson(jsonText, SavedTokens::class.java)
            val newTokens = migrationUtil.convertLegacySavedTokensToOtpTokens(savedTokens)
            otpTokenDatabase.otpTokenDao().insertAll(newTokens)
        }
    }

    suspend fun exportJsonFile(uri: Uri, password: CharArray? = null) {
        withContext(Dispatchers.IO) {
            val otpTokens = otpTokenDatabase.otpTokenDao().getAll().first()
            val legacyTokens = migrationUtil.convertOtpTokensToLegacyTokens(otpTokens)
            val tokenOrder = otpTokens.map {
                if (it.issuer != null) {
                    "${it.issuer}:${it.label}"
                } else {
                    it.label
                }
            }.toList()
            val jsonString = gson.toJson(SavedTokens(legacyTokens, tokenOrder))
            val out = if (password != null && password.isNotEmpty()) {
                BackupEncryption.encryptUtf8(jsonString, password, BackupPayloadType.JSON)
            } else {
                jsonString
            }
            writeUtf8(uri, out)
        }
    }

    suspend fun importKeyUriFile(fileUri: Uri, password: CharArray? = null) {
        withContext(Dispatchers.IO) {
            val raw = readUtf8(fileUri)
            val text = resolvePayload(raw, password, BackupPayloadType.KEY_URI)
            val currentLastOrdinal = otpTokenDatabase.otpTokenDao().getLastOrdinal() ?: 0
            val tokens = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, line ->
                    OtpTokenFactory.createFromUri(Uri.parse(line)).copy(
                        ordinal = currentLastOrdinal + index + 1
                    )
                }
                .toList()
            otpTokenDatabase.otpTokenDao().insertAll(tokens)
        }
    }

    suspend fun exportKeyUriFile(fileUri: Uri, password: CharArray? = null) {
        withContext(Dispatchers.IO) {
            val tokens = otpTokenDatabase.otpTokenDao().getAll().first()
            val plain = buildString {
                for (token in tokens) {
                    appendLine(OtpTokenFactory.toUri(token).toString())
                }
            }
            if (password != null && password.isNotEmpty()) {
                val encrypted = BackupEncryption.encryptUtf8(
                    plain,
                    password,
                    BackupPayloadType.KEY_URI
                )
                writeUtf8(fileUri, encrypted)
            } else {
                context.contentResolver.openOutputStream(fileUri, "w")?.use { outputStream ->
                    PrintWriter(outputStream).use { printWriter ->
                        printWriter.print(plain)
                    }
                }
            }
        }
    }

    /** True when [uri] content is an encrypted NetAigis backup container. */
    suspend fun isEncryptedBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        BackupEncryption.isEncryptedContainer(readUtf8(uri))
    }

    private fun resolvePayload(
        raw: String,
        password: CharArray?,
        expected: BackupPayloadType
    ): String {
        if (!BackupEncryption.isEncryptedContainer(raw)) {
            return raw
        }
        if (password == null || password.isEmpty()) {
            throw BackupEncryptionException("Password required for encrypted backup")
        }
        val (payloadType, plaintext) = BackupEncryption.decryptUtf8(raw, password)
        if (payloadType != expected) {
            throw BackupEncryptionException(
                "Backup payload is ${payloadType.wireName}, expected ${expected.wireName}"
            )
        }
        return plaintext
    }

    private fun readUtf8(uri: Uri): String {
        return context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Cannot open input: $uri" }
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).readText()
        }
    }

    private fun writeUtf8(uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri, "w").use { outputStream ->
            requireNotNull(outputStream) { "Cannot open output: $uri" }
            outputStream.write(text.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
