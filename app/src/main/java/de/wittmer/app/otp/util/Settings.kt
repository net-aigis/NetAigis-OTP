package de.wittmer.app.otp.util

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

private const val COPY_TO_CLIPBOARD_KEY = "copyToClipboard"
private const val REQUIRE_AUTHENTICATION = "requireAuthentication"

@Singleton
class Settings @Inject constructor(val sharedPreferences: SharedPreferences) {
    var copyToClipboard: Boolean
        get() = sharedPreferences.getBoolean(COPY_TO_CLIPBOARD_KEY, true)
        set(value) = sharedPreferences.edit().putBoolean(COPY_TO_CLIPBOARD_KEY, value).apply()
    var requireAuthentication: Boolean
        get() = sharedPreferences.getBoolean(REQUIRE_AUTHENTICATION, false)
        set(value) = sharedPreferences.edit().putBoolean(REQUIRE_AUTHENTICATION, value).apply()

}