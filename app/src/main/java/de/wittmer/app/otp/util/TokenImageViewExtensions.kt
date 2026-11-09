package de.wittmer.app.otp.util

import android.widget.ImageView
import de.wittmer.app.otp.textdrawable.TextDrawable
import de.wittmer.app.otp.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.OtpToken
import de.wittmer.app.otp.token_images.TokenImage
import de.wittmer.app.otp.token_images.matchToken
import de.wittmer.app.otp.token_images.matchTokenImage

fun ImageView.setTokenImage(token: OtpToken) {
    when {
        token.imagePath != null -> {
            Glide.with(this)
                .load(token.imagePath)
                .placeholder(R.drawable.logo)
                .into(this)
        }
        !token.iconKey.isNullOrBlank() -> {
            matchTokenImage(token.iconKey)?.let {
                setImageResource(it.resource)
            } ?: setFallbackTextImage(token)
        }
        else -> {
            matchThumbnail(token)?.let { setImageResource(it) }
                ?: setFallbackTextImage(token)
        }
    }
}

private fun ImageView.setFallbackTextImage(token: OtpToken) {
    val tokenText = token.displayName().substring(0, 1)
    val color = ColorGenerator.MATERIAL.getColor(tokenText)
    val tokenTextDrawable = TextDrawable.builder().buildRoundRect(tokenText, color, 10)
    setImageDrawable(tokenTextDrawable)
}

/**
 * Resolve a built-in thumbnail when Icon is unset. Alias wins over issuer so
 * users can keep a technical issuer (e.g. Ourcloud) while showing another
 * brand icon via alias (e.g. Nextcloud). Full priority: Icon → Alias → Issuer.
 */
private fun matchThumbnail(token: OtpToken): Int? {
    val keys = listOfNotNull(
        token.alias?.takeIf { it.isNotBlank() },
        token.issuer?.takeIf { it.isNotBlank() },
    )
    for (key in keys) {
        TokenImage.values().firstOrNull {
            it.matchToken(key, token.label)
        }?.resource?.let { return it }
    }
    return null
}
