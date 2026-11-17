package de.wittmer.app.otp.util

import android.app.Activity
import android.view.WindowManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.wittmer.app.otp.feature.FeatureAccess

/**
 * Applies [WindowManager.LayoutParams.FLAG_SECURE] unless screenshot mode is enabled.
 * Must run on every Activity window — FLAG_SECURE is per-window, not app-wide.
 */
fun Activity.applyScreenshotModeFromSettings() {
    val entryPoint = EntryPointAccessors.fromApplication(
        applicationContext,
        ScreenshotModeEntryPoint::class.java
    )
    val allowScreenshots =
        entryPoint.featureAccess().screenshotModeEnabled && entryPoint.settings().screenshotMode
    if (allowScreenshots) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScreenshotModeEntryPoint {
    fun featureAccess(): FeatureAccess
    fun settings(): Settings
}
