package de.wittmer.app.otp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.wittmer.app.otp.data.util.Converters

@Database(entities = [OtpToken::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class OtpTokenDatabase : RoomDatabase() {
    abstract fun otpTokenDao(): OtpTokenDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE otp_tokens ADD COLUMN category TEXT")
            }
        }
    }
}