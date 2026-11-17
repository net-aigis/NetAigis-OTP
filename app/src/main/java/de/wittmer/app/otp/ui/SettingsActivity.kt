package de.wittmer.app.otp.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.legacy.BackupEncryptionException
import de.wittmer.app.otp.data.legacy.ImportExportUtil
import de.wittmer.app.otp.data.legacy.wipe
import de.wittmer.app.otp.databinding.ActivitySettingsBinding
import de.wittmer.app.otp.databinding.DialogBackupPasswordBinding
import de.wittmer.app.otp.databinding.DialogImportPasswordBinding
import de.wittmer.app.otp.feature.FeatureAccess
import de.wittmer.app.otp.util.DocumentTreeWriter
import de.wittmer.app.otp.util.Settings
import de.wittmer.app.otp.util.StoragePermissionService
import de.wittmer.app.otp.util.UriDisplayNameReader
import de.wittmer.app.otp.util.applyScreenshotModeFromSettings
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    @Inject lateinit var importFromUtil: ImportExportUtil
    @Inject lateinit var settings: Settings
    @Inject lateinit var featureAccess: FeatureAccess

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var storagePermissionService: StoragePermissionService
    private lateinit var documentTreeWriter: DocumentTreeWriter
    private lateinit var uriDisplayNameReader: UriDisplayNameReader
    private var updatingSwitches = false
    private var pendingExportPassword: CharArray? = null

    private val dateFormatter: DateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    private val exportDirectoryPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Back / dismiss in the system picker → RESULT_CANCELED, keep previous folder.
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }
        val data = result.data ?: return@registerForActivityResult
        val uri = data.data ?: return@registerForActivityResult
        val takeFlags = data.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        try {
            storagePermissionService.persistTreePermission(uri, takeFlags)
            val usable = documentTreeWriter.ensureDirectory(uri).success ||
                storagePermissionService.hasPersistedPermission(uri)
            if (!usable) {
                storagePermissionService.clearTreePermission()
                Snackbar.make(
                    binding.rootView,
                    R.string.export_folder_unavailable,
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    binding.rootView,
                    R.string.export_folder_selected,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            refreshExportFolderLabel()
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot persist export folder", e)
            Snackbar.make(
                binding.rootView,
                R.string.export_folder_unavailable,
                Snackbar.LENGTH_LONG
            ).show()
            refreshExportFolderLabel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = ContextCompat.getColor(this, R.color.paper)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(paper, paper)
        )
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storagePermissionService = StoragePermissionService(this, settings)
        documentTreeWriter = DocumentTreeWriter(this)
        uriDisplayNameReader = UriDisplayNameReader(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Left/right: BaseActivity. Top/bottom: toolbar + scroll.
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
        refreshExportFolderLabel()
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
            applyScreenshotModeFromSettings()
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
        binding.exportFolderValue.setOnClickListener {
            showExportFolderChooserDialog()
        }
        binding.btnImportJson.setOnClickListener {
            performFileSearch(READ_JSON_REQUEST_CODE)
        }
        binding.btnImportKeyUri.setOnClickListener {
            performFileSearch(READ_KEY_URI_REQUEST_CODE)
        }
        binding.btnExportJson.setOnClickListener {
            startExport(
                titleRes = R.string.export_json_format,
                baseName = "netaigis-otp-backup",
                requestCode = WRITE_JSON_REQUEST_CODE
            )
        }
        binding.btnExportKeyUri.setOnClickListener {
            startExport(
                titleRes = R.string.export_key_uri_format,
                baseName = "netaigis-otp-keyuri",
                requestCode = WRITE_KEY_URI_REQUEST_CODE
            )
        }
    }

    /**
     * Password dialog first; plain key-uri keeps .txt, encrypted / JSON use .json.
     * With a configured tree URI (fahrtenbuch-style), write via DocumentFile; else SAF create dialog.
     */
    private fun startExport(titleRes: Int, baseName: String, requestCode: Int) {
        showExportPasswordDialog(titleRes) { password ->
            val useEncryption = password != null
            val effectiveMime = if (useEncryption || requestCode == WRITE_JSON_REQUEST_CODE) {
                "application/json"
            } else {
                "text/plain"
            }
            val effectiveExt = if (useEncryption || requestCode == WRITE_JSON_REQUEST_CODE) {
                "json"
            } else {
                "txt"
            }
            val displayName = "${baseName}_${dateFormatter.format(Date())}.$effectiveExt"
            val treeUriString = settings.exportFolderUri
            if (!treeUriString.isNullOrBlank()) {
                lifecycleScope.launch {
                    val treeUri = Uri.parse(treeUriString)
                    val ensure = documentTreeWriter.ensureDirectory(treeUri)
                    if (!ensure.success) {
                        storagePermissionService.clearTreePermission()
                        refreshExportFolderLabel()
                        password?.wipe()
                        Snackbar.make(
                            binding.rootView,
                            R.string.export_folder_unavailable,
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    val created = documentTreeWriter.createTextFile(
                        treeUri,
                        displayName,
                        effectiveMime
                    )
                    val fileUri = created.uri
                    if (!created.success || fileUri == null) {
                        password?.wipe()
                        Snackbar.make(
                            binding.rootView,
                            R.string.export_folder_unavailable,
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    runExport(requestCode, fileUri, password)
                }
            } else {
                pendingExportPassword = password
                createFile(effectiveMime, baseName, effectiveExt, requestCode)
            }
        }
    }

    private fun showExportPasswordDialog(titleRes: Int, onConfirmed: (CharArray?) -> Unit) {
        val dialogBinding = DialogBackupPasswordBinding.inflate(LayoutInflater.from(this))

        fun updateEncryptUi() {
            val encrypt = dialogBinding.switchEncrypt.isChecked
            dialogBinding.passwordLayout.isEnabled = encrypt
            dialogBinding.password.isEnabled = encrypt
            dialogBinding.checkConfirmPassword.isEnabled = encrypt
            dialogBinding.passwordHint.visibility = if (encrypt) View.VISIBLE else View.GONE
            dialogBinding.checkConfirmPassword.visibility =
                if (encrypt) View.VISIBLE else View.GONE
            if (!encrypt) {
                dialogBinding.password.text = null
                dialogBinding.passwordConfirm.text = null
                dialogBinding.checkConfirmPassword.isChecked = false
            }
            updateConfirmVisibility(dialogBinding)
        }

        dialogBinding.switchEncrypt.setOnCheckedChangeListener { _, _ -> updateEncryptUi() }
        dialogBinding.checkConfirmPassword.setOnCheckedChangeListener { _, _ ->
            updateConfirmVisibility(dialogBinding)
        }
        updateEncryptUi()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.export_text, null)
            .setNegativeButton(R.string.cancel_text, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                if (!dialogBinding.switchEncrypt.isChecked) {
                    dialog.dismiss()
                    onConfirmed(null)
                    return@setOnClickListener
                }
                val password = dialogBinding.password.text?.toString()?.toCharArray() ?: charArrayOf()
                if (password.isEmpty()) {
                    dialogBinding.passwordLayout.error =
                        getString(R.string.backup_password_required)
                    return@setOnClickListener
                }
                if (dialogBinding.checkConfirmPassword.isChecked) {
                    val confirm =
                        dialogBinding.passwordConfirm.text?.toString()?.toCharArray()
                            ?: charArrayOf()
                    try {
                        if (!password.contentEquals(confirm)) {
                            dialogBinding.passwordConfirmLayout.error =
                                getString(R.string.backup_password_mismatch)
                            return@setOnClickListener
                        }
                    } finally {
                        confirm.wipe()
                    }
                }
                dialog.dismiss()
                onConfirmed(password)
            }
        }
        dialogBinding.password.doAfterTextChanged {
            dialogBinding.passwordLayout.error = null
        }
        dialogBinding.passwordConfirm.doAfterTextChanged {
            dialogBinding.passwordConfirmLayout.error = null
        }
        dialog.show()
    }

    private fun updateConfirmVisibility(dialogBinding: DialogBackupPasswordBinding) {
        val showConfirm = dialogBinding.switchEncrypt.isChecked &&
            dialogBinding.checkConfirmPassword.isChecked
        dialogBinding.passwordConfirmLayout.visibility =
            if (showConfirm) View.VISIBLE else View.GONE
        dialogBinding.passwordConfirmLayout.isEnabled = showConfirm
        dialogBinding.passwordConfirm.isEnabled = showConfirm
        if (!showConfirm) {
            dialogBinding.passwordConfirm.text = null
            dialogBinding.passwordConfirmLayout.error = null
        }
    }

    private fun showImportPasswordDialog(onConfirmed: (CharArray) -> Unit) {
        val dialogBinding = DialogImportPasswordBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_password_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ok_text, null)
            .setNegativeButton(R.string.cancel_text, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val password = dialogBinding.password.text?.toString()?.toCharArray() ?: charArrayOf()
                if (password.isEmpty()) {
                    dialogBinding.password.error = getString(R.string.backup_password_required)
                    return@setOnClickListener
                }
                dialog.dismiss()
                onConfirmed(password)
            }
        }
        dialog.show()
    }

    private fun showExportFolderChooserDialog() {
        // Own dialog (fahrtenbuch-style): cancel / clear without entering the system tree UI.
        val titlePadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            resources.displayMetrics
        ).toInt()
        val titleView = TextView(this).apply {
            text = getString(R.string.export_folder_dialog_title)
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(titlePadding, titlePadding, titlePadding, titlePadding / 2)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setCustomTitle(titleView)
            .setMessage(R.string.export_folder_dialog_text)
            .setPositiveButton(R.string.export_folder_choose) { _, _ ->
                launchExportDirectoryPicker()
            }
            .setNegativeButton(R.string.cancel_text, null)
            .setNeutralButton(R.string.export_folder_clear) { _, _ ->
                storagePermissionService.clearTreePermission()
                refreshExportFolderLabel()
            }
            .create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).isEnabled =
            !settings.exportFolderUri.isNullOrBlank()
    }

    private fun launchExportDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }
        try {
            exportDirectoryPicker.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Cannot find activity", e)
            Toast.makeText(
                applicationContext,
                getString(R.string.launch_file_browser_failure),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshExportFolderLabel() {
        val uriString = settings.exportFolderUri
        if (uriString.isNullOrBlank()) {
            settings.exportFolderDisplayName = null
            binding.exportFolderValue.setText(R.string.export_folder_not_set)
            return
        }
        val uri = Uri.parse(uriString)
        // Always derive from the tree URI so we show Download/Export, not only Export.
        val name = uriDisplayNameReader.treeLabel(uri)
            ?: getString(R.string.export_folder_label)
        settings.exportFolderDisplayName = name
        binding.exportFolderValue.setText(name)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode != Activity.RESULT_OK) {
            pendingExportPassword?.wipe()
            pendingExportPassword = null
            return
        }

        when (requestCode) {
            WRITE_JSON_REQUEST_CODE, WRITE_KEY_URI_REQUEST_CODE -> {
                val uri = resultData?.data ?: return
                val password = pendingExportPassword
                pendingExportPassword = null
                lifecycleScope.launch {
                    runExport(requestCode, uri, password)
                }
            }

            READ_JSON_REQUEST_CODE -> {
                val uri = resultData?.data ?: return
                prepareImport(uri, isJson = true)
            }

            READ_KEY_URI_REQUEST_CODE -> {
                val uri = resultData?.data ?: return
                prepareImport(uri, isJson = false)
            }
        }
    }

    private suspend fun runExport(requestCode: Int, uri: Uri, password: CharArray?) {
        try {
            when (requestCode) {
                WRITE_JSON_REQUEST_CODE -> importFromUtil.exportJsonFile(uri, password)
                WRITE_KEY_URI_REQUEST_CODE -> importFromUtil.exportKeyUriFile(uri, password)
            }
            Snackbar.make(binding.rootView, R.string.export_succeeded_text, Snackbar.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Snackbar.make(binding.rootView, R.string.export_failed_text, Snackbar.LENGTH_SHORT)
                .show()
        } finally {
            password?.wipe()
        }
    }

    private fun prepareImport(uri: Uri, isJson: Boolean) {
        lifecycleScope.launch {
            val encrypted = try {
                importFromUtil.isEncryptedBackup(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot read import file", e)
                Snackbar.make(
                    binding.rootView,
                    if (isJson) R.string.import_json_failed_text else R.string.import_key_uri_failed_text,
                    Snackbar.LENGTH_SHORT
                ).show()
                return@launch
            }
            if (encrypted) {
                showImportPasswordDialog { password ->
                    if (isJson) {
                        confirmAndImportJson(uri, password)
                    } else {
                        runKeyUriImport(uri, password)
                    }
                }
            } else if (isJson) {
                confirmAndImportJson(uri, null)
            } else {
                runKeyUriImport(uri, null)
            }
        }
    }

    private fun confirmAndImportJson(uri: Uri, password: CharArray?) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_json_file)
            .setMessage(R.string.import_json_file_warning)
            .setIcon(R.drawable.alert)
            .setPositiveButton(R.string.ok_text) { _: DialogInterface, _: Int ->
                lifecycleScope.launch {
                    try {
                        importFromUtil.importJsonFile(uri, password)
                        Snackbar.make(
                            binding.rootView,
                            R.string.import_succeeded_text,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: BackupEncryptionException) {
                        Log.e(TAG, "Import JSON decrypt failed", e)
                        Snackbar.make(
                            binding.rootView,
                            R.string.backup_decrypt_failed,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Import JSON failed", e)
                        Snackbar.make(
                            binding.rootView,
                            R.string.import_json_failed_text,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } finally {
                        password?.wipe()
                    }
                }
            }
            .setNegativeButton(R.string.cancel_text) { _, _ -> password?.wipe() }
            .setOnCancelListener { password?.wipe() }
            .show()
    }

    private fun runKeyUriImport(uri: Uri, password: CharArray?) {
        lifecycleScope.launch {
            try {
                importFromUtil.importKeyUriFile(uri, password)
                Snackbar.make(
                    binding.rootView,
                    R.string.import_succeeded_text,
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: BackupEncryptionException) {
                Log.e(TAG, "Import Key uri decrypt failed", e)
                Snackbar.make(
                    binding.rootView,
                    R.string.backup_decrypt_failed,
                    Snackbar.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Import Key uri failed", e)
                Snackbar.make(
                    binding.rootView,
                    R.string.import_key_uri_failed_text,
                    Snackbar.LENGTH_SHORT
                ).show()
            } finally {
                password?.wipe()
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
            pendingExportPassword?.wipe()
            pendingExportPassword = null
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
