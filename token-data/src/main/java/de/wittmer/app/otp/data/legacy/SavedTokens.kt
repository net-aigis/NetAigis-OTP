package de.wittmer.app.otp.data.legacy

import de.wittmer.app.otp.data.legacy.Token

/**
 * The saved tokens to storage
 */
data class SavedTokens(val tokens: List<Token>, val tokenOrder: List<String>)
