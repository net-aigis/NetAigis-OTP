package de.wittmer.app.otp.uitest

import android.net.Uri
import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import de.wittmer.app.otp.ImportExportEntryPoint
import de.wittmer.app.otp.data.OtpTokenFactory
import de.wittmer.app.otp.data.legacy.SavedTokens
import de.wittmer.app.otp.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4ClassRunner::class)
class ImportExportRoundTripTest {

    @Test
    fun keyUriAndJsonRoundTrip() {
        launchActivity<MainActivity>().use {
            runBlocking {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val accessor = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ImportExportEntryPoint::class.java
                )
                val util = accessor.importExportUtil()
                val dao = accessor.otpTokenDatabase().otpTokenDao()
                val migrationUtil = accessor.migrationUtil()
                val gson = accessor.gson()

                if (dao.getAll().first().isEmpty()) {
                    val testContext = InstrumentationRegistry.getInstrumentation().context
                    val sample = testContext.assets.open("sample-tokens.txt").bufferedReader().readLines()
                        .filter { it.isNotBlank() }
                        .mapIndexed { index, line ->
                            OtpTokenFactory.createFromUri(Uri.parse(line.trim())).copy(
                                ordinal = index + 1L
                            )
                        }
                    dao.insertAll(sample)
                }

                val before = dao.getAll().first()
                assertTrue("Need tokens before export", before.isNotEmpty())
                val beforeKeys = before.map { fingerprint(it) }.sorted()

                // Key URI round-trip
                val keyUriFile = File(context.cacheDir, "roundtrip-keyuri.txt")
                keyUriFile.printWriter().use { writer ->
                    before.forEach { writer.println(OtpTokenFactory.toUri(it)) }
                }
                util.exportKeyUriFile(Uri.fromFile(File(context.cacheDir, "roundtrip-keyuri-util.txt")))

                before.forEach { dao.deleteById(it.id) }
                assertEquals(0, dao.getAll().first().size)

                util.importKeyUriFile(Uri.fromFile(keyUriFile))
                val afterKeyUri = dao.getAll().first()
                assertEquals("Key URI round-trip mismatch", beforeKeys, afterKeyUri.map { fingerprint(it) }.sorted())

                // JSON round-trip
                val jsonFile = File(context.cacheDir, "roundtrip.json")
                val legacy = migrationUtil.convertOtpTokensToLegacyTokens(afterKeyUri)
                val order = afterKeyUri.map { token ->
                    if (!token.issuer.isNullOrBlank()) "${token.issuer}:${token.label}" else token.label
                }
                jsonFile.writeText(gson.toJson(SavedTokens(legacy, order)))
                util.exportJsonFile(Uri.fromFile(File(context.cacheDir, "roundtrip-util.json")))

                afterKeyUri.forEach { dao.deleteById(it.id) }
                assertEquals(0, dao.getAll().first().size)

                util.importJsonFile(Uri.fromFile(jsonFile))
                val afterJson = dao.getAll().first()
                assertEquals("JSON round-trip mismatch", beforeKeys, afterJson.map { fingerprint(it) }.sorted())
            }
        }
    }

    private fun fingerprint(token: de.wittmer.app.otp.data.OtpToken): String =
        listOf(
            token.issuer.orEmpty(),
            token.label,
            token.alias.orEmpty(),
            token.iconKey.orEmpty(),
            // Base32 padding differs between URI import and JSON encode/decode; ignore '='
            token.secret.trimEnd('='),
            token.tokenType.name,
            token.algorithm.uppercase(),
            token.digits,
            token.period,
            token.counter
        ).joinToString("|")
}
