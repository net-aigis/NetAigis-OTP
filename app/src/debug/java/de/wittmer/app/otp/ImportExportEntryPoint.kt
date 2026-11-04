package de.wittmer.app.otp

import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.wittmer.app.otp.data.MigrationUtil
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.data.legacy.ImportExportUtil

/** Debug-only accessors for instrumented export/import tests. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImportExportEntryPoint {
    fun importExportUtil(): ImportExportUtil
    fun otpTokenDatabase(): OtpTokenDatabase
    fun migrationUtil(): MigrationUtil
    fun gson(): Gson
}
