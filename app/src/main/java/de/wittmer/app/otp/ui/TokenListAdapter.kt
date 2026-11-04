package de.wittmer.app.otp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.ArrayMap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.OtpToken
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.data.OtpTokenType
import de.wittmer.app.otp.data.legacy.TokenCode
import de.wittmer.app.otp.token.TokenLayout
import de.wittmer.app.otp.data.util.TokenCodeUtil
import de.wittmer.app.otp.util.Settings
import javax.inject.Inject

@ActivityScoped
class TokenListAdapter @Inject constructor(@ActivityContext private val context: Context,
                                           private val otpTokenDatabase: OtpTokenDatabase,
                                           private val tokenCodeUtil: TokenCodeUtil,
                                           private val settings: Settings) : ListAdapter<OtpToken, TokenViewHolder>(TokenItemCallback()) {
    val activity = context as AppCompatActivity
    private val clipboardManager: ClipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val tokenCodes: MutableMap<Long, TokenCode> = ArrayMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val tokenLayout = LayoutInflater.from(activity).inflate(R.layout.token, parent, false) as TokenLayout
        return TokenViewHolder(activity, tokenLayout)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        val currentToken = currentList[position]
        holder.bind(currentToken)
        holder.tokenLayout.setOnClickListener { v ->
            activity.lifecycleScope.launch {
                // Fetch the token again to refresh the HOTP token. This is needed because
                // incrementCounter will not refresh the token after HOTP update
                otpTokenDatabase.otpTokenDao().get(currentToken.id).first() ?.let { token ->
                    val codes = tokenCodeUtil.generateTokenCode(token)

                    if (token.tokenType == OtpTokenType.HOTP) {
                        otpTokenDatabase.otpTokenDao().incrementCounter(token.id)
                    }

                    if (settings.copyToClipboard) {
                        // Copy code to clipboard.
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, codes.currentCode))
                        Snackbar.make(v, R.string.code_copied, Snackbar.LENGTH_SHORT).show()
                    }

                    tokenCodes[token.id] = codes

                    (v as TokenLayout).start(token.tokenType, codes, true)
                }
            }
        }
    }
}