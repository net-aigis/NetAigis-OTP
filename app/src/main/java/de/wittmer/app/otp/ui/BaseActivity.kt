package de.wittmer.app.otp.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.wittmer.app.otp.util.applyScreenshotModeFromSettings

/**
 * App-wide Activity base:
 * - Screenshot-mode → FLAG_SECURE
 * - Horizontal system-bar insets (landscape nav buttons / cutouts) on the content root
 *
 * Vertical insets (status / bottom nav / IME) stay screen-specific so toolbars and
 * bottom action rows can keep their own padding.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenshotModeFromSettings()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        installHorizontalSystemBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        installHorizontalSystemBarInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        installHorizontalSystemBarInsets()
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotModeFromSettings()
    }

    /**
     * Pads [android.R.id.content] left/right so child inset listeners cannot wipe this out.
     */
    private fun installHorizontalSystemBarInsets() {
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, view.paddingTop, bars.right, view.paddingBottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(content)
    }
}
