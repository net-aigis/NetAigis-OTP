package de.wittmer.app.otp.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

private const val COPY_TO_CLIPBOARD_KEY = "copyToClipboard"
private const val REQUIRE_AUTHENTICATION = "requireAuthentication"
private const val SCREENSHOT_MODE_KEY = "screenshotMode"

@Singleton
class Settings @Inject constructor(val sharedPreferences: SharedPreferences) {
    var copyToClipboard: Boolean
        get() = sharedPreferences.getBoolean(COPY_TO_CLIPBOARD_KEY, true)
        set(value) = sharedPreferences.edit().putBoolean(COPY_TO_CLIPBOARD_KEY, value).apply()
    var requireAuthentication: Boolean
        get() = sharedPreferences.getBoolean(REQUIRE_AUTHENTICATION, false)
        set(value) = sharedPreferences.edit().putBoolean(REQUIRE_AUTHENTICATION, value).apply()
    /** When true, FLAG_SECURE is cleared so mirrors/screenshots work (OTP-visible risk). */
    var screenshotMode: Boolean
        get() = sharedPreferences.getBoolean(SCREENSHOT_MODE_KEY, false)
        set(value) = sharedPreferences.edit().putBoolean(SCREENSHOT_MODE_KEY, value).apply()

}