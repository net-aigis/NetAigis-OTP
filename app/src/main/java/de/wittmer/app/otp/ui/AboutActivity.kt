/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2013  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.wittmer.app.otp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import de.wittmer.app.otp.R
import de.wittmer.app.otp.databinding.AboutBinding

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
    private lateinit var binding: AboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = ContextCompat.getColor(this, R.color.paper)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(paper, paper)
        )
        binding = AboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, bars.top, 0, 0)
            val pad = resources.getDimensionPixelSize(R.dimen.space_1)
            binding.scrollContent.setPadding(pad, pad, pad, pad + bars.bottom)
            windowInsets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val info = packageManager.getPackageInfo(packageName, 0)
        val versionText = getString(
            R.string.about_version_value,
            info.versionName,
            PackageInfoCompat.getLongVersionCode(info)
        )
        binding.appVersion.text = versionText

        binding.btnAppVersion.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("version", versionText))
            Toast.makeText(this, R.string.about_version_copied, Toast.LENGTH_SHORT).show()
        }

        binding.btnChangelog.setOnClickListener {
            startActivity(
                LegalDocumentActivity.intent(
                    this,
                    R.string.screen_changelog,
                    R.raw.changelog
                )
            )
        }
        binding.btnGithub.setOnClickListener { openUrl(GITHUB) }
        binding.btnEmail.setOnClickListener { openMail(getString(R.string.provider_email)) }
        binding.btnContactWebsite.setOnClickListener { openUrl(WEBSITE) }
        binding.btnRate.setOnClickListener { openUrl(PLAY_STORE) }
        binding.btnDonate.setOnClickListener { openUrl(DONATE) }

        binding.btnLicense.setOnClickListener {
            startActivity(
                LegalDocumentActivity.intent(
                    this,
                    R.string.screen_app_license,
                    R.raw.app_license
                )
            )
        }
        binding.btnOpenSourceLicenses.setOnClickListener {
            startActivity(
                LegalDocumentActivity.intent(
                    this,
                    R.string.screen_open_source_licenses,
                    R.raw.open_source_licenses
                )
            )
        }
        binding.btnImprint.setOnClickListener {
            startActivity(
                LegalDocumentActivity.intent(
                    this,
                    R.string.screen_imprint,
                    R.raw.legal_notice
                )
            )
        }
        binding.btnPrivacy.setOnClickListener {
            startActivity(
                LegalDocumentActivity.intent(
                    this,
                    R.string.screen_privacy,
                    R.raw.privacy_policy
                )
            )
        }

        binding.aboutCopyright.movementMethod = LinkMovementMethod.getInstance()
        binding.aboutCopyright.text = HtmlCompat.fromHtml(
            getString(R.string.about_copyright),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun openMail(email: String) {
        startActivity(
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }
        )
    }

    companion object {
        private const val WEBSITE = "https://net-aigis.de/"
        private const val GITHUB = "https://github.com/net-aigis/NetAigis-OTP"
        private const val PLAY_STORE =
            "https://play.google.com/store/apps/details?id=de.wittmer.app.otp"
        private const val DONATE = "https://net-aigis.de/"
    }
}
