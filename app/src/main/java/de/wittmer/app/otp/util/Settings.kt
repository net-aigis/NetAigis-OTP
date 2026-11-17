package de.wittmer.app.otp.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

private const val COPY_TO_CLIPBOARD_KEY = "copyToClipboard"
private const val REQUIRE_AUTHENTICATION = "requireAuthentication"
private const val SCREENSHOT_MODE_KEY = "screenshotMode"
/** Same key name idea as fahrtenbuch (`export_tree_uri`). */
private const val EXPORT_FOLDER_URI_KEY = "export_tree_uri"
private const val EXPORT_FOLDER_NAME_KEY = "export_tree_display_name"

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

    /** Persisted SAF tree URI for export destination, or null if unset. */
    var exportFolderUri: String?
        get() = sharedPreferences.getString(EXPORT_FOLDER_URI_KEY, null)
        set(value) {
            sharedPreferences.edit().putString(EXPORT_FOLDER_URI_KEY, value).apply()
        }

    /** Cached folder label for the settings TextField (set when the user picks a tree). */
    var exportFolderDisplayName: String?
        get() = sharedPreferences.getString(EXPORT_FOLDER_NAME_KEY, null)
        set(value) {
            sharedPreferences.edit().putString(EXPORT_FOLDER_NAME_KEY, value).apply()
        }
}