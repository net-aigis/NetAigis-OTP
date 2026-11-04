package de.wittmer.app.otp.feature

/**
 * Central edition/feature gate. Flavor-specific implementations live in
 * `app/src/free` and `app/src/pro`. Call sites must use this API instead of
 * checking [de.wittmer.app.otp.BuildConfig.FLAVOR] directly.
 */
interface FeatureAccess {
    /** Allow clearing FLAG_SECURE for mirrors/screenshots (OTP-visible risk). */
    val screenshotModeEnabled: Boolean
}
