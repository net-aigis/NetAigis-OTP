package de.wittmer.app.otp.feature

/**
 * Central edition/feature gate. Flavor-specific implementations live in
 * `app/src/free` and `app/src/pro`. Call sites must use this API instead of
 * checking [de.wittmer.app.otp.BuildConfig.FLAVOR] directly.
 *
 * Concrete Pro feature flags are not defined yet; implementations stay open
 * until the Pro feature list is decided.
 */
interface FeatureAccess
