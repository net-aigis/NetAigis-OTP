package de.wittmer.app.otp.util

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.graphics.Typeface
import androidx.annotation.RawRes
import de.wittmer.app.otp.BuildConfig
import de.wittmer.app.otp.R
import java.util.regex.Pattern

object LegalMarkdown {
    private val LINK_PATTERN: Pattern =
        Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)|(https?://[^\\s)]+)")

    fun loadDocument(context: Context, @RawRes rawResId: Int): CharSequence {
        val markdown = context.resources.openRawResource(rawResId)
            .bufferedReader()
            .use { it.readText() }
            .replaceLegalPlaceholders(context)
        return render(markdown)
    }

    private fun String.replaceLegalPlaceholders(context: Context): String {
        val replacements = mapOf(
            "@@app_name@@" to context.getString(R.string.app_name),
            "@@application_id@@" to BuildConfig.APPLICATION_ID,
            "@@provider_company_name@@" to context.getString(R.string.provider_company_name),
            "@@provider_represented_by@@" to context.getString(R.string.provider_represented_by),
            "@@provider_address@@" to context.getString(R.string.provider_address),
            "@@provider_address_city@@" to context.getString(R.string.provider_address_city),
            "@@provider_tax_id@@" to context.getString(R.string.provider_tax_id),
            "@@provider_vat_number@@" to context.getString(R.string.provider_vat_number),
            "@@provider_email_label@@" to context.getString(R.string.provider_email_label),
            "@@provider_email@@" to context.getString(R.string.provider_email),
            "@@provider_website@@" to context.getString(R.string.provider_website),
            "@@responsible_for_content_email_label@@" to
                context.getString(R.string.responsible_for_content_email_label),
            "@@responsible_for_content_email@@" to
                context.getString(R.string.responsible_for_content_email)
        )
        return replacements.entries.fold(this) { text, (placeholder, value) ->
            text.replace(placeholder, value)
        }
    }

    private fun render(markdown: String): CharSequence {
        val builder = SpannableStringBuilder()
        val paragraphLines = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphLines.isEmpty()) {
                return
            }
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            appendInline(builder, paragraphLines.joinToString("\n"))
            paragraphLines.clear()
        }

        fun appendHeading(text: String, relativeSize: Float) {
            flushParagraph()
            if (builder.isNotEmpty()) {
                builder.append("\n\n")
            }
            val start = builder.length
            appendInline(builder, text)
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                RelativeSizeSpan(relativeSize),
                start,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        markdown.lines().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isBlank() -> flushParagraph()
                line.startsWith("# ") -> appendHeading(line.removePrefix("# "), 1.35f)
                line.startsWith("## ") -> appendHeading(line.removePrefix("## "), 1.15f)
                else -> paragraphLines += line
            }
        }
        flushParagraph()
        return builder
    }

    private fun appendInline(builder: SpannableStringBuilder, text: String) {
        val matcher = LINK_PATTERN.matcher(text)
        var lastIndex = 0
        while (matcher.find()) {
            builder.append(text.substring(lastIndex, matcher.start()))
            val label = matcher.group(1) ?: matcher.group(3).orEmpty()
            val uri = matcher.group(2) ?: matcher.group(3).orEmpty()
            val start = builder.length
            builder.append(label)
            builder.setSpan(URLSpan(uri), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            lastIndex = matcher.end()
        }
        builder.append(text.substring(lastIndex))
    }
}
