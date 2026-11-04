package de.wittmer.app.otp.feature

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureAccessImpl @Inject constructor() : FeatureAccess {
    override val screenshotModeEnabled = false
}
