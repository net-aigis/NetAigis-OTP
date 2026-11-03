package de.wittmer.app.otp.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.databinding.DeleteBinding
import de.wittmer.app.otp.util.setTokenImage
import javax.inject.Inject

@AndroidEntryPoint
class DeleteActivity : AppCompatActivity() {
    @Inject lateinit var otpTokenDatabase: OtpTokenDatabase
    private lateinit var binding: DeleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DeleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonLayout = binding.root.findViewById<View>(R.id.button_layout)
        ViewCompat.setOnApplyWindowInsetsListener(buttonLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )
            windowInsets
        }

        lifecycleScope.launch {
            val tokenId = intent.getLongExtra(EXTRA_TOKEN_ID, 0L)

            if (tokenId == 0L) {
                return@launch
            }

            binding.cancel.setOnClickListener {
                finish()
            }

            binding.delete.setOnClickListener {
                lifecycleScope.launch {
                    otpTokenDatabase.otpTokenDao().deleteById(tokenId)
                    setResult(RESULT_OK)
                }
            }

            otpTokenDatabase.otpTokenDao().get(tokenId).collect { token ->
                if (token == null) {
                    finish()
                    return@collect
                }

                (findViewById<View>(R.id.issuer) as TextView).text = token.issuer
                (findViewById<View>(R.id.label) as TextView).text = token.label
                binding.imageView.setTokenImage(token)
            }

        }
    }

    companion object {
        const val EXTRA_TOKEN_ID = "token_id"
    }
}
