package de.wittmer.app.otp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.wittmer.app.otp.util.applyScreenshotModeFromSettings

/**
 * Ensures screenshot protection follows the Screenshot-mode setting on every screen.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenshotModeFromSettings()
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotModeFromSettings()
    }
}
