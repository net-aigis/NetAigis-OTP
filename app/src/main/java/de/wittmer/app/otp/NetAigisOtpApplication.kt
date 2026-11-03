package de.wittmer.app.otp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import de.wittmer.app.otp.util.Settings
import javax.inject.Inject

@HiltAndroidApp
class NetAigisOtpApplication: Application() {
    @Inject lateinit var settings: Settings

    override fun onCreate() {
        super.onCreate()
        if (settings.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}