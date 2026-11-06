/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2014  Nathaniel McCallum, Red Hat
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.EncryptionType
import de.wittmer.app.otp.data.OtpToken
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.data.OtpTokenType
import de.wittmer.app.otp.databinding.EditBinding
import de.wittmer.app.otp.token_images.TokenImage
import de.wittmer.app.otp.token_images.displayKey
import de.wittmer.app.otp.util.ImageUtil
import de.wittmer.app.otp.util.Settings
import de.wittmer.app.otp.util.blankToNull
import de.wittmer.app.otp.util.setTokenImage
import javax.inject.Inject

@AndroidEntryPoint
class EditActivity : AppCompatActivity(), TextWatcher, View.OnClickListener {

    @Inject lateinit var otpTokenDatabase: OtpTokenDatabase
    @Inject lateinit var imageUtil: ImageUtil
    @Inject lateinit var settings: Settings

    private lateinit var binding: EditBinding
    private lateinit var mIssuer: EditText
    private lateinit var mLabel: EditText
    private lateinit var mAlias: EditText
    private lateinit var mCategory: AutoCompleteTextView
    private lateinit var mIconKey: AutoCompleteTextView
    private lateinit var mImage: ImageButton
    private lateinit var mRestore: Button
    private lateinit var mSave: Button

    private var mIssuerCurrent: String? = null
    private var mIssuerDefault: String? = null
    private var mLabelCurrent: String? = null
    private var mLabelDefault: String? = null
    private var mAliasCurrent: String? = null
    private var mAliasDefault: String? = null
    private var mCategoryCurrent: String? = null
    private var mCategoryDefault: String? = null
    private var mIconKeyCurrent: String? = null
    private var mIconKeyDefault: String? = null

    private var mImageCurrent: Uri? = null
    private var mImageDefault: Uri? = null
    private var mImageDisplay: Uri? = null

    private var tokenId: Long = 0L
    private var issuerEditConfirmed = false
    private var issuerWarningShowing = false

    private fun showImage(imageUri: Uri?) {
        mImageDisplay = imageUri
        onTextChanged(null, 0, 0, 0)

        if (imageUri != null) {
            Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.logo)
                .into(mImage)
        } else {
            // Same issuer thumbnail / letter fallback as the token list
            mImage.setTokenImage(tokenForImageDisplay())
        }

        // Remove user image option is only enabled if there is a custom image
        binding.removeUserTokenImage.isEnabled = mImageDisplay != null
    }

    private fun tokenForImageDisplay(): OtpToken {
        val issuer = when {
            ::mIssuer.isInitialized -> mIssuer.text.toString()
            else -> mIssuerCurrent
        }
        val label = when {
            ::mLabel.isInitialized -> mLabel.text.toString()
            else -> mLabelCurrent.orEmpty()
        }
        val alias = when {
            ::mAlias.isInitialized -> mAlias.text.toString().blankToNull()
            else -> mAliasCurrent.blankToNull()
        }
        val iconKey = when {
            ::mIconKey.isInitialized -> mIconKey.text.toString().blankToNull()
            else -> mIconKeyCurrent.blankToNull()
        }
        return OtpToken(
            id = tokenId,
            ordinal = 0,
            issuer = issuer,
            label = label,
            imagePath = null,
            alias = alias,
            iconKey = iconKey,
            tokenType = OtpTokenType.TOTP,
            algorithm = "SHA1",
            secret = "",
            digits = 6,
            counter = 0,
            period = 30,
            encryptionType = EncryptionType.PLAIN_TEXT,
        )
    }

    private fun imageIs(uri: Uri?): Boolean {
        return if (uri == null) mImageDisplay == null else uri == mImageDisplay
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = EditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val buttonLayout = binding.root.findViewById<View>(R.id.button_layout)
        ViewCompat.setOnApplyWindowInsetsListener(buttonLayout) { view, windowInsets ->
            // Keep Cancel/Save above the nav bar and the IME keyboard.
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                insets.bottom
            )
            windowInsets
        }

        lifecycleScope.launch {
            tokenId = intent.getLongExtra(EXTRA_TOKEN_ID, 0)

            // Get token values.
            val token = otpTokenDatabase.otpTokenDao().get(tokenId).first() ?: return@launch
            mIssuerCurrent = token.issuer
            mLabelCurrent = token.label
            mAliasCurrent = token.alias ?: ""
            mCategoryCurrent = token.category ?: ""
            mIconKeyCurrent = token.iconKey ?: ""
            mImageCurrent = if (token.imagePath != null) Uri.parse(token.imagePath) else null
            mIssuerDefault = token.issuer
            mLabelDefault = token.label
            mAliasDefault = mAliasCurrent
            mCategoryDefault = mCategoryCurrent
            mIconKeyDefault = mIconKeyCurrent
            mImageDefault = mImageCurrent

            // Get references to widgets.
            mIssuer = findViewById<View>(R.id.issuer) as EditText
            mLabel = findViewById<View>(R.id.label) as EditText
            mAlias = findViewById<View>(R.id.alias) as EditText
            mCategory = findViewById<View>(R.id.category) as AutoCompleteTextView
            mIconKey = findViewById<View>(R.id.icon_key) as AutoCompleteTextView
            mImage = findViewById<View>(R.id.image) as ImageButton
            mRestore = findViewById<View>(R.id.restore) as Button
            mSave = findViewById<View>(R.id.save) as Button
            applyBrandButtonStyle(findViewById(R.id.cancel))
            applyBrandButtonStyle(mSave)
            applyBrandButtonStyle(mRestore)
            applyBrandButtonStyle(binding.removeUserTokenImage)
            setSupportActionBar(findViewById(R.id.toolbar))

            // Setup text changed listeners.
            mIssuer.addTextChangedListener(this@EditActivity)
            mLabel.addTextChangedListener(this@EditActivity)
            mAlias.addTextChangedListener(this@EditActivity)
            mCategory.addTextChangedListener(this@EditActivity)
            mIconKey.addTextChangedListener(this@EditActivity)

            // Setup click callbacks.
            findViewById<View>(R.id.cancel).setOnClickListener(this@EditActivity)
            findViewById<View>(R.id.save).setOnClickListener(this@EditActivity)
            findViewById<View>(R.id.restore).setOnClickListener(this@EditActivity)
            binding.removeUserTokenImage.setOnClickListener(this@EditActivity)
            mImage.setOnClickListener(this@EditActivity)

            // Issuer/label first so issuer thumbnail matching works when no custom image exists
            mLabel.setText(mLabelCurrent)
            mIssuer.setText(mIssuerCurrent)
            mAlias.setText(mAliasCurrent)
            mCategory.setText(mCategoryCurrent)
            mIconKey.setText(mIconKeyCurrent)
            mIssuer.setSelection(mIssuer.text.length)
            mIssuer.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && !issuerEditConfirmed) {
                    showIssuerEditWarning()
                }
            }
            showImage(mImageCurrent)

            // Token details
            val algorithmTextView = findViewById<TextView>(R.id.algorithm)
            algorithmTextView.text = token.algorithm

            val digitsTextView = findViewById<TextView>(R.id.digits)
            digitsTextView.text = token.digits.toString()

            val secretTextView = findViewById<TextView>(R.id.secret)
            if (settings.requireAuthentication) {
                secretTextView.text = getString(R.string.click_to_display)
                secretTextView.setOnClickListener {
                    val executor = ContextCompat.getMainExecutor(this@EditActivity)
                    val biometricPrompt = BiometricPrompt(this@EditActivity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationError(errorCode: Int,
                                                                   errString: CharSequence) {
                                    super.onAuthenticationError(errorCode, errString)
                                    // Don't show error message toast if user pressed back button
                                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                                        Toast.makeText(applicationContext,
                                            "${getString(R.string.authentication_error)} $errString", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }

                                override fun onAuthenticationSucceeded(
                                        result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    secretTextView.text = token.secret
                                    secretTextView.setOnClickListener(null)
                                }

                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                    Toast.makeText(applicationContext,
                                        R.string.unable_to_authenticate, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.authentication_dialog_title))
                            .setSubtitle(getString(R.string.authentication_dialog_subtitle))
                            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                            .build()

                    biometricPrompt.authenticate(promptInfo)
                }
            } else {
                secretTextView.text = token.secret
            }

            val intervalTextView = findViewById<TextView>(R.id.interval)
            intervalTextView.text = token.period.toString()

            val categories = otpTokenDatabase.otpTokenDao().getAllCategories().first().toMutableList()
            categories.add(0, getString(R.string.uncategorized))
            val adapter = ArrayAdapter(this@EditActivity, android.R.layout.simple_dropdown_item_1line, categories)
            mCategory.setAdapter(adapter)
            mCategory.threshold = 0
            mCategory.setOnClickListener {
                mCategory.showDropDown()
            }
            mCategory.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    mCategory.showDropDown()
                }
            }
            mCategory.setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    mCategory.setText("")
                }
            }

            setupIconKeyAdapter()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                data?.data?.let {
                    lifecycleScope.launch {
                        val path = imageUtil.saveImageUriToFile(it)
                        showImage(path)
                    }
                } ?: let {
                    Toast.makeText(this, R.string.fail_to_add_image, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val label = mLabel.text.toString()
        val issuer = mIssuer.text.toString()
        val alias = mAlias.text.toString()
        val category = mCategory.text.toString()
        val iconKey = mIconKey.text.toString()
        // Keep buttons enabled visually (same lime as Cancel); gate actions via clickable.
        val saveAllowed = label != mLabelCurrent || issuer != mIssuerCurrent || alias != mAliasCurrent || category != mCategoryCurrent || iconKey != mIconKeyCurrent || !imageIs(mImageCurrent)
        val restoreAllowed = label != mLabelDefault || issuer != mIssuerDefault || alias != mAliasDefault || category != mCategoryDefault || iconKey != mIconKeyDefault || !imageIs(mImageDefault)
        mSave.isEnabled = true
        mRestore.isEnabled = true
        mSave.isClickable = saveAllowed
        mRestore.isClickable = restoreAllowed
        applyBrandButtonStyle(mSave)
        applyBrandButtonStyle(mRestore)
    }

    private fun showIssuerEditWarning() {
        if (issuerWarningShowing) {
            return
        }
        issuerWarningShowing = true
        mIssuer.clearFocus()
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.issuer_edit_warning)
            .setPositiveButton(R.string.ok_text) { _, _ ->
                issuerEditConfirmed = true
                mIssuer.requestFocus()
                mIssuer.setSelection(mIssuer.text.length)
            }
            .setNegativeButton(R.string.cancel_text) { _, _ ->
                issuerEditConfirmed = false
                mAlias.requestFocus()
                mAlias.setSelection(mAlias.text.length)
            }
            .setOnDismissListener {
                issuerWarningShowing = false
            }
            .show()
    }

    private fun applyBrandButtonStyle(button: Button) {
        button.background = ContextCompat.getDrawable(this, R.drawable.brand_button_bg)
        button.backgroundTintList = null
        button.setTextColor(ContextCompat.getColor(this, R.color.primary))
        button.alpha = 1f
    }

    override fun afterTextChanged(s: Editable) {
        // Keep issuer thumbnail in sync while editing, unless a custom image is set
        if (mImageDisplay == null && ::mImage.isInitialized) {
            mImage.setTokenImage(tokenForImageDisplay())
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.image -> {
                val intent = Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, IMAGE_REQUEST_CODE)
            }

            R.id.restore -> {
                mLabel.setText(mLabelDefault)
                mIssuer.setText(mIssuerDefault)
                mAlias.setText(mAliasDefault)
                mCategory.setText(mCategoryDefault)
                mIconKey.setText(mIconKeyDefault)
                mIssuer.setSelection(mIssuer.text.length)
                showImage(mImageDefault)
            }

            R.id.save -> {
                lifecycleScope.launch {
                    val token = otpTokenDatabase.otpTokenDao().get(tokenId).first() ?: return@launch
                    val newToken = token.copy(
                        issuer = mIssuer.text.toString(),
                        label = mLabel.text.toString(),
                        alias = mAlias.text.toString().blankToNull(),
                        category = mCategory.text.toString().blankToNull(),
                        iconKey = mIconKey.text.toString().blankToNull(),
                        imagePath = mImageDisplay?.toString()
                    )

                    otpTokenDatabase.otpTokenDao().update(newToken)
                    setResult(RESULT_OK)
                    finish()
                }
            }

            R.id.remove_user_token_image -> {
                showImage(null)
            }

            R.id.cancel -> finish()
        }
    }

    private fun setupIconKeyAdapter() {
        val iconKeys = TokenImage.values().map { it.displayKey() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, iconKeys)
        mIconKey.setAdapter(adapter)
        mIconKey.threshold = 0
        mIconKey.setOnClickListener {
            mIconKey.showDropDown()
        }
        mIconKey.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                mIconKey.showDropDown()
            }
        }
    }

    companion object {
        const val EXTRA_TOKEN_ID = "token_id"
        const val IMAGE_REQUEST_CODE = 60
    }
}
