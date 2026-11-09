package de.wittmer.app.otp.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.wittmer.app.otp.R
import de.wittmer.app.otp.databinding.ActivityLegalDocumentBinding
import de.wittmer.app.otp.util.LegalMarkdown

class LegalDocumentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLegalDocumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = ContextCompat.getColor(this, R.color.paper)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(paper, paper)
        )
        binding = ActivityLegalDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, bars.top, 0, 0)
            val pad = resources.getDimensionPixelSize(R.dimen.space_3)
            binding.documentText.setPadding(pad, pad, pad, pad + bars.bottom)
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

        binding.documentText.movementMethod = InternalLegalLinkMovementMethod
        binding.documentText.text = LegalMarkdown.loadDocument(this, rawRes)
    }

    private object InternalLegalLinkMovementMethod : LinkMovementMethod() {
        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable,
            event: MotionEvent
        ): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                val offset = touchOffset(widget, event)
                if (offset >= 0) {
                    val url = buffer.getSpans(offset, offset, URLSpan::class.java)
                        .firstOrNull()
                        ?.url
                    if (url != null && openInternalDocument(widget.context, url)) {
                        return true
                    }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }

        private fun touchOffset(widget: TextView, event: MotionEvent): Int {
            val layout = widget.layout ?: return -1
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            if (y < 0 || y > layout.height) {
                return -1
            }
            val line = layout.getLineForVertical(y)
            if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) {
                return -1
            }
            return layout.getOffsetForHorizontal(line, x.toFloat())
        }
    }

    companion object {
        private const val EXTRA_TITLE_RES = "title_res"
        private const val EXTRA_RAW_RES = "raw_res"
        private const val INTERNAL_LEGAL_SCHEME = "netaigisotp"
        private const val INTERNAL_LEGAL_HOST = "legal"

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

        private fun openInternalDocument(context: Context, url: String): Boolean {
            val uri = Uri.parse(url)
            if (uri.scheme != INTERNAL_LEGAL_SCHEME || uri.host != INTERNAL_LEGAL_HOST) {
                return false
            }
            val document = uri.pathSegments.firstOrNull() ?: return false
            val target = when (document) {
                "open_source_licenses" ->
                    R.string.screen_open_source_licenses to R.raw.open_source_licenses
                else -> return false
            }
            context.startActivity(intent(context, target.first, target.second))
            return true
        }
    }
}
