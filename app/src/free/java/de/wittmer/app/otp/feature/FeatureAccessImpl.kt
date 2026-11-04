package de.wittmer.app.otp.feature

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureAccessImpl @Inject constructor() : FeatureAccess {
    // Same feature set as Pro for now.
    override val screenshotModeEnabled = true
}
