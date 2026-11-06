package de.wittmer.app.otp.util

import android.app.Activity
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.wittmer.app.otp.R

/**
 * Binds optional "?" help buttons next to Add/Edit form labels.
 * Texts follow the Google Authenticator Key URI Format and common OTP practice.
 */
object FormFieldHelp {

    private data class Help(
        @param:IdRes val buttonId: Int,
        @param:StringRes val titleRes: Int,
        @param:StringRes val messageRes: Int,
    )

    private val HELP = listOf(
        Help(R.id.help_issuer, R.string.issuer, R.string.field_help_issuer),
        Help(R.id.help_account, R.string.account, R.string.field_help_account),
        Help(R.id.help_alias, R.string.alias, R.string.field_help_alias),
        Help(R.id.help_category, R.string.category, R.string.field_help_category),
        Help(R.id.help_icon, R.string.icon, R.string.field_help_icon),
        Help(R.id.help_secret, R.string.secret, R.string.field_help_secret),
        Help(R.id.help_type, R.string.type, R.string.field_help_type),
        Help(R.id.help_digits, R.string.digits, R.string.field_help_digits),
        Help(R.id.help_algorithm, R.string.algorithm, R.string.field_help_algorithm),
        Help(R.id.help_interval, R.string.interval, R.string.field_help_interval),
        Help(R.id.help_counter, R.string.counter, R.string.field_help_counter),
    )

    fun bind(activity: Activity) {
        for (help in HELP) {
            val button = activity.findViewById<View>(help.buttonId) ?: continue
            button.setOnClickListener {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(help.titleRes)
                    .setMessage(help.messageRes)
                    .setPositiveButton(R.string.ok_text, null)
                    .show()
            }
        }
    }
}
