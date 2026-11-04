package de.wittmer.app.otp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NetAigisOtpApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // Brand UI is dark-only; no user dark-mode toggle.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
