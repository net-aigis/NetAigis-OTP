package de.wittmer.app.otp.util

import de.wittmer.app.otp.data.OtpToken

fun OtpToken.displayName(): String {
    return issuer?.takeIf { it.isNotBlank() }
        ?: label
}

fun String?.blankToNull(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() }
}
