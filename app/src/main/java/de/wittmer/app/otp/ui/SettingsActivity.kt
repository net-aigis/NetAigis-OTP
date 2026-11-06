package de.wittmer.app.otp.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.legacy.ImportExportUtil
import de.wittmer.app.otp.databinding.ActivitySettingsBinding
import de.wittmer.app.otp.feature.FeatureAccess
import de.wittmer.app.otp.util.Settings
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject lateinit var importFromUtil: ImportExportUtil
    @Inject lateinit var settings: Settings
    @Inject lateinit var featureAccess: FeatureAccess

    private lateinit var binding: ActivitySettingsBinding
    private var updatingSwitches = false

    private val dateFormatter: DateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = ContextCompat.getColor(this, R.color.paper)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(paper, paper)
        )
        binding = ActivitySettingsBinding.inflate(layoutInflater)
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

        bindBehaviorSwitches()
        bindImportExport()
    }

    private fun bindBehaviorSwitches() {
        updatingSwitches = true
        binding.switchCopyToClipboard.isChecked = settings.copyToClipboard

        val screenshotEnabled = featureAccess.screenshotModeEnabled
        binding.switchScreenshotMode.isEnabled = screenshotEnabled
        binding.switchScreenshotMode.isChecked = screenshotEnabled && settings.screenshotMode
        binding.screenshotModeHint.visibility = if (screenshotEnabled) View.GONE else View.VISIBLE

        binding.switchRequireAuthentication.isChecked = settings.requireAuthentication
        updatingSwitches = false

        binding.switchCopyToClipboard.setOnCheckedChangeListener { _, isChecked ->
            if (!updatingSwitches) {
                settings.copyToClipboard = isChecked
            }
        }

        binding.switchScreenshotMode.setOnCheckedChangeListener { _, isChecked ->
            if (updatingSwitches) {
                return@setOnCheckedChangeListener
            }
            if (!featureAccess.screenshotModeEnabled) {
                updatingSwitches = true
                binding.switchScreenshotMode.isChecked = false
                updatingSwitches = false
                return@setOnCheckedChangeListener
            }
            settings.screenshotMode = isChecked
        }

        binding.switchRequireAuthentication.setOnCheckedChangeListener { _, isChecked ->
            if (updatingSwitches) {
                return@setOnCheckedChangeListener
            }
            // Revert until biometric confirms; then apply.
            updatingSwitches = true
            binding.switchRequireAuthentication.isChecked = !isChecked
            updatingSwitches = false
            if (isChecked) {
                verifyAuthentication(isEnabling = true)
            } else {
                verifyAuthentication(isDisabling = true)
            }
        }
    }

    private fun bindImportExport() {
        binding.btnImportJson.setOnClickListener {
            performFileSearch(READ_JSON_REQUEST_CODE)
        }
        binding.btnImportKeyUri.setOnClickListener {
            performFileSearch(READ_KEY_URI_REQUEST_CODE)
        }
        binding.btnExportJson.setOnClickListener {
            createFile("application/json", "netaigis-otp-backup", "json", WRITE_JSON_REQUEST_CODE)
        }
        binding.btnExportKeyUri.setOnClickListener {
            createFile("text/plain", "netaigis-otp-backup", "txt", WRITE_KEY_URI_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            WRITE_JSON_REQUEST_CODE -> {
                lifecycleScope.launch {
                    val uri = resultData?.data ?: return@launch
                    importFromUtil.exportJsonFile(uri)
                    Snackbar.make(binding.rootView, R.string.export_succeeded_text, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            READ_JSON_REQUEST_CODE -> {
                val uri = resultData?.data ?: return
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.import_json_file)
                    .setMessage(R.string.import_json_file_warning)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.ok_text) { _: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            try {
                                importFromUtil.importJsonFile(uri)
                                Snackbar.make(
                                    binding.rootView,
                                    R.string.import_succeeded_text,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Log.e(TAG, "Import JSON failed", e)
                                Snackbar.make(
                                    binding.rootView,
                                    R.string.import_json_failed_text,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel_text, null)
                    .show()
            }

            WRITE_KEY_URI_REQUEST_CODE -> {
                lifecycleScope.launch {
                    val uri = resultData?.data ?: return@launch
                    importFromUtil.exportKeyUriFile(uri)
                    Snackbar.make(binding.rootView, R.string.export_succeeded_text, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            READ_KEY_URI_REQUEST_CODE -> {
                lifecycleScope.launch {
                    val uri = resultData?.data ?: return@launch
                    try {
                        importFromUtil.importKeyUriFile(uri)
                        Snackbar.make(
                            binding.rootView,
                            R.string.import_succeeded_text,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Import Key uri failed", e)
                        Snackbar.make(
                            binding.rootView,
                            R.string.import_key_uri_failed_text,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun performFileSearch(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Cannot find activity", e)
            Toast.makeText(
                applicationContext,
                getString(R.string.launch_file_browser_failure),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createFile(
        mimeType: String,
        fileName: String,
        fileExtension: String,
        requestCode: Int
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, "${fileName}_${dateFormatter.format(Date())}.$fileExtension")
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Cannot find activity", e)
            Toast.makeText(
                applicationContext,
                getString(R.string.launch_file_browser_failure),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun verifyAuthentication(isEnabling: Boolean = false, isDisabling: Boolean = false) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(
                            applicationContext,
                            "${getString(R.string.authentication_error)} $errString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    updatingSwitches = true
                    if (isEnabling) {
                        settings.requireAuthentication = true
                        binding.switchRequireAuthentication.isChecked = true
                    } else if (isDisabling) {
                        settings.requireAuthentication = false
                        binding.switchRequireAuthentication.isChecked = false
                    }
                    updatingSwitches = false
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext,
                        R.string.unable_to_authenticate,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.authentication_dialog_title))
            .setSubtitle(getString(R.string.authentication_dialog_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        private val TAG = SettingsActivity::class.java.simpleName
        private const val READ_JSON_REQUEST_CODE = 42
        private const val WRITE_JSON_REQUEST_CODE = 43
        private const val READ_KEY_URI_REQUEST_CODE = 44
        private const val WRITE_KEY_URI_REQUEST_CODE = 45
    }
}
