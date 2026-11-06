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

/*
 * Portions Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.wittmer.app.otp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import de.wittmer.app.otp.R
import de.wittmer.app.otp.data.MigrationUtil
import de.wittmer.app.otp.data.OtpTokenDatabase
import de.wittmer.app.otp.data.OtpTokenFactory
import de.wittmer.app.otp.databinding.MainBinding
import de.wittmer.app.otp.feature.FeatureAccess
import de.wittmer.app.otp.util.Settings
import de.wittmer.app.otp.util.applyFixedScaleBackground
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var settings: Settings
    @Inject lateinit var featureAccess: FeatureAccess
    @Inject lateinit var tokenMigrationUtil: MigrationUtil
    @Inject lateinit var otpTokenDatabase: OtpTokenDatabase
    @Inject lateinit var tokenListAdapter: TokenListAdapter

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: MainBinding
    private var searchQuery = ""
    private var lastSessionEndTimestamp = 0L;
    private var categories: List<String> = emptyList()
    private var hasUncategorizedTokens = false
    private var selectedCategoryFilter: String? = null
    private var updatingCategoryTabs = false

    private val tokenListObserver: AdapterDataObserver = object: AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            binding.tokenList.scrollToPosition(positionStart)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onNewIntent(intent)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appBackground.applyFixedScaleBackground(R.drawable.logo_watermark_wide)

        ViewCompat.setOnApplyWindowInsetsListener(binding.tokenList) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                resources.getDimensionPixelSize(R.dimen.token_list_bottom_padding) + insets.bottom
            )
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabBar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val marginParams = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            marginParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.space_3) + insets.bottom
            if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                marginParams.leftMargin = resources.getDimensionPixelSize(R.dimen.space_3) + insets.left
            } else {
                marginParams.rightMargin = resources.getDimensionPixelSize(R.dimen.space_3) + insets.right
            }
            view.layoutParams = marginParams
            windowInsets
        }


        viewModel.migrateOldData()

        binding.tokenList.adapter = tokenListAdapter

        // Used GridlayoutManager to support tablet mode for multiple columns
        // Make sure one column has at least 320 DP
        val columns =  max(1, resources.configuration.screenWidthDp / 320)
        binding.tokenList.layoutManager = GridLayoutManager(this, columns)


        ItemTouchHelper(TokenTouchCallback(this, tokenListAdapter, otpTokenDatabase))
            .attachToRecyclerView(binding.tokenList)
        tokenListAdapter.registerAdapterDataObserver(tokenListObserver)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getTokenList().collect { tokens ->
                        tokenListAdapter.submitList(tokens)

                        if (tokens.isEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                            binding.tokenList.visibility = View.GONE
                        } else {
                            binding.emptyView.visibility = View.GONE
                            binding.tokenList.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    viewModel.getAuthState().collect { authState ->
                        if (authState == MainViewModel.AuthState.UNAUTHENTICATED) {
                            verifyAuthentication()
                        }
                    }
                }

                launch {
                    // Combined so tabs never rebuild with half-loaded category state
                    // (that was resetting the filter on rotation).
                    combine(
                        viewModel.getAllCategories(),
                        viewModel.hasUncategorizedTokens()
                    ) { cats, hasUncategorized ->
                        cats to hasUncategorized
                    }.collect { (cats, hasUncategorized) ->
                        categories = cats
                        hasUncategorizedTokens = hasUncategorized
                        updateCategoryTabs()
                        invalidateOptionsMenu()
                    }
                }
            }
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setTitleTextColor(getColor(R.color.on_banner))
        binding.toolbar.overflowIcon?.setTint(getColor(R.color.on_banner))

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener, androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setTokenSearchQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                searchQuery = query ?: ""
                viewModel.setTokenSearchQuery(query ?: "")
                return true
            }

        })

        binding.categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (!updatingCategoryTabs) {
                    selectCategoryFilter(tab.tag as? String, updateTabs = false)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        binding.addTokenFab.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
        binding.scanTokenFab.setOnClickListener {
            startActivity(Intent(this, ScanTokenActivity::class.java))
        }

        // Pro shortcut can turn screenshot mode on; Free always stays secure.
        if (featureAccess.screenshotModeEnabled &&
            intent.extras?.getBoolean(SCREENSHOT_MODE_EXTRA) == true
        ) {
            settings.screenshotMode = true
        }
        applyScreenshotMode()
    }

    private fun applyScreenshotMode() {
        val allowScreenshots = featureAccess.screenshotModeEnabled && settings.screenshotMode
        if (allowScreenshots) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tokenListAdapter.unregisterAdapterDataObserver(tokenListObserver)
        lastSessionEndTimestamp = 0L;
    }

    override fun onStart() {
        super.onStart()

        viewModel.onSessionStart()
    }

    override fun onResume() {
        super.onResume()
        applyScreenshotMode()
    }
    
    override fun onStop() {
        super.onStop()
        viewModel.onSessionStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val iconTint = getColor(R.color.on_banner)
        for (i in 0 until menu.size()) {
            menu.getItem(i).icon?.setTint(iconTint)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val filterItem = menu.findItem(R.id.action_filter)
        val filterMenu = filterItem?.subMenu ?: return super.onPrepareOptionsMenu(menu)
        filterItem.isVisible = categories.isNotEmpty() || hasUncategorizedTokens
        filterMenu.clear()

        if (categories.isNotEmpty()) {
            filterMenu.add(Menu.NONE, R.id.action_filter_all, Menu.NONE, R.string.all_categories)
        }
        if (hasUncategorizedTokens) {
            filterMenu.add(Menu.NONE, R.id.action_filter_uncategorized, Menu.NONE, R.string.uncategorized)
        }

        for (category in categories) {
            filterMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, category).setOnMenuItemClickListener {
                selectCategoryFilter(category)
                true
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }

            R.id.action_filter_all -> {
                selectCategoryFilter(null)
                return true
            }

            R.id.action_filter_uncategorized -> {
                selectCategoryFilter(MainViewModel.CATEGORY_UNCATEGORIZED)
                return true
            }

            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                return true
            }

            R.id.quit_and_lock -> {
                finish()
                return true
            }
        }

        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (packageName == intent.extras?.getString(SHARE_FROM_PACKAGE_NAME_INTENT_EXTRA)) {
            Log.i(TAG, "Intent shared from the same package name. Ignoring the intent and do not add the token")
            return
        }

        val uri = intent.data
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    otpTokenDatabase.otpTokenDao().insert(OtpTokenFactory.createFromUri(uri))
                } catch (e: Exception) {
                    Snackbar.make(binding.rootView, R.string.invalid_token_uri_received, Snackbar.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    private fun selectCategoryFilter(category: String?, updateTabs: Boolean = true) {
        selectedCategoryFilter = category
        viewModel.setCategoryFilter(category)

        if (updateTabs) {
            selectCategoryTab(category)
        }
    }

    private fun updateCategoryTabs() {
        // ViewModel survives rotation; Activity field does not — sync before rebuilding tabs.
        selectedCategoryFilter = viewModel.getCategoryFilter()
        normalizeCategoryFilter()

        updatingCategoryTabs = true
        binding.categoryTabs.removeAllTabs()
        if (categories.isEmpty() && !hasUncategorizedTokens) {
            binding.categoryTabs.visibility = View.GONE
            // Defer clear so late TabLayout selection callbacks cannot reset the filter.
            binding.categoryTabs.post { updatingCategoryTabs = false }
            return
        }

        binding.categoryTabs.visibility = View.VISIBLE
        if (categories.isNotEmpty()) {
            addCategoryTab(getString(R.string.category_tab_all), null)
        }
        if (hasUncategorizedTokens) {
            addCategoryTab(getString(R.string.category_tab_none), MainViewModel.CATEGORY_UNCATEGORIZED)
        }
        categories.forEach { category ->
            addCategoryTab(category, category)
        }
        selectCategoryTab(selectedCategoryFilter)
        // Re-apply after TabLayout finishes any pending layout/selection work.
        binding.categoryTabs.post {
            selectedCategoryFilter = viewModel.getCategoryFilter()
            selectCategoryTab(selectedCategoryFilter)
            updatingCategoryTabs = false
        }
    }

    private fun normalizeCategoryFilter() {
        val normalizedCategory = when {
            categories.isEmpty() && !hasUncategorizedTokens -> null
            categories.isEmpty() && hasUncategorizedTokens -> MainViewModel.CATEGORY_UNCATEGORIZED
            selectedCategoryFilter == MainViewModel.CATEGORY_UNCATEGORIZED && !hasUncategorizedTokens -> null
            selectedCategoryFilter != null &&
                    selectedCategoryFilter != MainViewModel.CATEGORY_UNCATEGORIZED &&
                    !categories.contains(selectedCategoryFilter) -> null
            else -> selectedCategoryFilter
        }

        if (selectedCategoryFilter != normalizedCategory) {
            selectedCategoryFilter = normalizedCategory
            viewModel.setCategoryFilter(normalizedCategory)
        }
    }

    private fun addCategoryTab(title: String, category: String?) {
        val tab = binding.categoryTabs.newTab()
            .setText(title)
        tab.tag = category
        binding.categoryTabs.addTab(tab, category == selectedCategoryFilter)
    }

    private fun selectCategoryTab(category: String?) {
        for (index in 0 until binding.categoryTabs.tabCount) {
            val tab = binding.categoryTabs.getTabAt(index) ?: continue
            if (tab.tag == category) {
                tab.select()
                return
            }
        }
    }

    private fun verifyAuthentication() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
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

                        if (errorCode != BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                            finish()
                        }
                    }

                    override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.setAuthState(MainViewModel.AuthState.AUTHENTICATED)
                    }

                    override fun onAuthenticationFailed() {
                        // Invalid authentication, e.g. wrong fingerprint. Android auth UI shows an
                        // error, so no need for FreeOTP to show one
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

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val SCREENSHOT_MODE_EXTRA = "screenshot_mode"
        const val SHARE_FROM_PACKAGE_NAME_INTENT_EXTRA = "shareFromPackageName"
    }
}
