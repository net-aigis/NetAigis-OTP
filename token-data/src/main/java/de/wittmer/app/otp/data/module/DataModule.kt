package de.wittmer.app.otp.data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.room.Room
import com.google.gson.Gson
import de.wittmer.app.otp.data.OtpTokenDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun database(@ApplicationContext context:Context)
            = Room.databaseBuilder(context, OtpTokenDatabase::class.java, "otp-token-db")
        .addMigrations(OtpTokenDatabase.MIGRATION_1_2)
        .build()

    @Singleton
    @Provides
    fun gson() = Gson()
}
