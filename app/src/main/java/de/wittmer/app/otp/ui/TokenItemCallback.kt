package de.wittmer.app.otp.ui

import androidx.recyclerview.widget.DiffUtil
import de.wittmer.app.otp.data.OtpToken
import de.wittmer.app.otp.data.legacy.Token

class TokenItemCallback: DiffUtil.ItemCallback<OtpToken>() {
    override fun areItemsTheSame(oldItem: OtpToken, newItem: OtpToken): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: OtpToken, newItem: OtpToken): Boolean {
        return oldItem == newItem
    }
}