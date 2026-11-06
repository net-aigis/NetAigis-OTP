package de.wittmer.app.otp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.wittmer.app.otp.R
import de.wittmer.app.otp.databinding.ActivityLegalDocumentBinding
import de.wittmer.app.otp.util.LegalMarkdown

class LegalDocumentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLegalDocumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLegalDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.documentScroll) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )
            windowInsets
        }

        val titleRes = intent.getIntExtra(EXTRA_TITLE_RES, 0)
        val rawRes = intent.getIntExtra(EXTRA_RAW_RES, 0)
        if (titleRes == 0 || rawRes == 0) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setTitle(titleRes)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.documentText.movementMethod = LinkMovementMethod.getInstance()
        binding.documentText.text = LegalMarkdown.loadDocument(this, rawRes)
    }

    companion object {
        private const val EXTRA_TITLE_RES = "title_res"
        private const val EXTRA_RAW_RES = "raw_res"

        fun intent(
            context: Context,
            @StringRes titleRes: Int,
            @RawRes rawRes: Int
        ): Intent {
            return Intent(context, LegalDocumentActivity::class.java).apply {
                putExtra(EXTRA_TITLE_RES, titleRes)
                putExtra(EXTRA_RAW_RES, rawRes)
            }
        }
    }
}
