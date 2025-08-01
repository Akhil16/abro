package com.example.abro.ui.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.abro.R
import com.example.abro.data.models.TabInfo
import com.example.abro.data.repositories.BookmarkRepository
import com.example.abro.data.repositories.SettingsRepository
import com.example.abro.data.repositories.TabRepository
import com.example.abro.ui.adapters.TabSpinnerAdapter
import com.example.abro.ui.webview.WebViewManager
import com.example.abro.utils.DownloadHelper
import com.example.abro.utils.UrlUtils
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), WebViewManager.WebViewListener {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var backBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var urlBar: EditText
    private lateinit var tabSpinner: Spinner
    private lateinit var btnAddTab: ImageButton
    private lateinit var btnCloseTab: ImageButton
    private lateinit var btnBookmark: ImageButton
    private lateinit var btnMenu: ImageButton

    private val webViews = mutableListOf<WebView>()
    private val tabInfos = mutableListOf<TabInfo>()
    private var currentTabIndex = 0
    
    private lateinit var spinnerAdapter: TabSpinnerAdapter
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var tabRepository: TabRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var webViewManager: WebViewManager
    private lateinit var downloadHelper: DownloadHelper

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val bookmarkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val url = result.data?.getStringExtra("url")
            if (url != null) {
                getCurrentWebView()?.loadUrl(url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply night mode setting
        settingsRepository = SettingsRepository(this)
        if (settingsRepository.isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRepositories()
        initViews()
        setupTabManagement()
        setupNavigation()
        setupMenuActions()
        loadSavedTabs()
        setupBackPressHandling()
    }

    private fun initRepositories() {
        bookmarkRepository = BookmarkRepository(this)
        tabRepository = TabRepository(this)
        downloadHelper = DownloadHelper(this)
        webViewManager = WebViewManager(this, downloadHelper, uiScope)
    }

    private fun initViews() {
        webViewContainer = findViewById(R.id.webview_container)
        backBtn = findViewById(R.id.btn_back)
        refreshBtn = findViewById(R.id.btn_refresh)
        urlBar = findViewById(R.id.url_bar)
        tabSpinner = findViewById(R.id.tab_spinner)
        btnAddTab = findViewById(R.id.btn_add_tab)
        btnCloseTab = findViewById(R.id.btn_close_tab)
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnMenu = findViewById(R.id.btn_menu)
    }

    private fun setupTabManagement() {
        spinnerAdapter = TabSpinnerAdapter(this, tabInfos)
        tabSpinner.adapter = spinnerAdapter

        tabSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position != currentTabIndex && position in webViews.indices) {
                    switchToTab(position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnAddTab.setOnClickListener { showTabOptions() }
        btnCloseTab.setOnClickListener { closeCurrentTab() }
    }

    private fun setupNavigation() {
        backBtn.setOnClickListener {
            getCurrentWebView()?.let {
                if (it.canGoBack()) it.goBack()
            }
        }

        refreshBtn.setOnClickListener {
            getCurrentWebView()?.reload()
        }

        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUrlFromBar()
                true
            } else false
        }
    }

    private fun setupMenuActions() {
        btnBookmark.setOnClickListener { toggleBookmark() }
        btnMenu.setOnClickListener { showMenu() }
    }

    private fun loadSavedTabs() {
        val savedTabs = tabRepository.loadTabUrls()
        if (savedTabs.isNotEmpty()) {
            for ((index, url) in savedTabs.withIndex()) {
                addTab(url, switchTo = index == 0)
            }
        } else {
            addTab("https://www.google.com")
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                getCurrentWebView()?.let {
                    if (it.canGoBack()) it.goBack() else finish()
                } ?: finish()
            }
        })
    }

    private fun showTabOptions() {
        val options = arrayOf("New Tab", "New Incognito Tab")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Add Tab")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> addTab("https://www.google.com", isIncognito = false)
                1 -> addTab("https://www.google.com", isIncognito = true)
            }
        }
        builder.show()
    }

    private fun addTab(initialUrl: String, switchTo: Boolean = true, isIncognito: Boolean = false) {
        val webView = webViewManager.createWebView(initialUrl, isIncognito, this)
        
        webViews.add(webView)
        tabInfos.add(
            TabInfo(
                title = UrlUtils.getDomainFromUrl(initialUrl),
                favicon = null,
                url = initialUrl,
                isIncognito = isIncognito
            )
        )
        
        spinnerAdapter.notifyDataSetChanged()
        saveTabsToPreferences()
        
        if (switchTo) switchToTab(webViews.size - 1)
    }

    private fun switchToTab(index: Int) {
        if (index !in webViews.indices) return
        
        webViewContainer.removeAllViews()
        webViewContainer.addView(webViews[index])
        currentTabIndex = index
        
        urlBar.setText(webViews[index].url)
        updateNavButtons()
        updateBookmarkButton()
        
        if (tabSpinner.selectedItemPosition != index) {
            tabSpinner.setSelection(index)
        }
        
        saveTabsToPreferences()
    }

    private fun closeCurrentTab() {
        if (webViews.size <= 1) {
            Toast.makeText(this, "Cannot close the last tab.", Toast.LENGTH_SHORT).show()
            return
        }
        
        closeTab(currentTabIndex)
    }

    private fun closeTab(index: Int) {
        if (index !in webViews.indices) return

        webViewContainer.removeView(webViews[index])
        webViews[index].destroy()
        webViews.removeAt(index)
        tabInfos.removeAt(index)
        spinnerAdapter.notifyDataSetChanged()

        if (webViews.isEmpty()) {
            addTab("https://www.google.com", true)
            return
        }
        
        val newIndex = if (index == 0) 0 else index - 1
        switchToTab(newIndex)
        saveTabsToPreferences()
    }

    private fun toggleBookmark() {
        val currentUrl = getCurrentWebView()?.url ?: return
        val currentTitle = getCurrentWebView()?.title ?: UrlUtils.getDomainFromUrl(currentUrl)
        
        if (bookmarkRepository.isBookmarked(currentUrl)) {
            bookmarkRepository.removeBookmark(currentUrl)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkRepository.addBookmark(currentTitle, currentUrl)
            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
        }
        updateBookmarkButton()
    }

    private fun showMenu() {
        val options = arrayOf("Bookmarks", "Settings", "Share", "Find in Page")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Menu")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openBookmarks()
                1 -> openSettings()
                2 -> shareCurrentPage()
                3 -> findInPage()
            }
        }
        builder.show()
    }

    private fun openBookmarks() {
        val intent = Intent(this, BookmarksActivity::class.java)
        bookmarkLauncher.launch(intent)
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun shareCurrentPage() {
        val currentUrl = getCurrentWebView()?.url ?: return
        val currentTitle = getCurrentWebView()?.title ?: ""
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$currentTitle\n$currentUrl")
            putExtra(Intent.EXTRA_SUBJECT, currentTitle)
        }
        startActivity(Intent.createChooser(shareIntent, "Share page"))
    }

    private fun findInPage() {
        getCurrentWebView()?.showFindDialog(null, true)
    }

    private fun getCurrentWebView(): WebView? = webViews.getOrNull(currentTabIndex)

    private fun loadUrlFromBar() {
        val url = UrlUtils.formatUrlForSearch(urlBar.text.toString())
        getCurrentWebView()?.loadUrl(url)
    }

    private fun updateNavButtons() {
        backBtn.isEnabled = getCurrentWebView()?.canGoBack() == true
        refreshBtn.isEnabled = getCurrentWebView() != null
    }

    private fun updateBookmarkButton() {
        val currentUrl = getCurrentWebView()?.url ?: return
        if (bookmarkRepository.isBookmarked(currentUrl)) {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun saveTabsToPreferences() {
        val urls = webViews.map { it.url ?: "" }
        tabRepository.saveTabUrls(urls)
    }

    // WebViewManager.WebViewListener implementation
    override fun onPageFinished(webView: WebView, url: String?) {
        val idx = webViews.indexOf(webView)
        if (idx != -1) {
            tabInfos[idx].url = url ?: ""
            if (currentTabIndex == idx) {
                urlBar.setText(url)
                updateNavButtons()
                updateBookmarkButton()
            }
            
            val title = webView.title ?: UrlUtils.getDomainFromUrl(url ?: "")
            tabInfos[idx].title = title
            spinnerAdapter.notifyDataSetChanged()
        }
    }

    override fun onFaviconReceived(webView: WebView, icon: Bitmap?) {
        val idx = webViews.indexOf(webView)
        if (icon != null && idx != -1) {
            tabInfos[idx].favicon = icon
            spinnerAdapter.notifyDataSetChanged()
        }
    }

    override fun onTitleChanged(webView: WebView, title: String?) {
        val idx = webViews.indexOf(webView)
        if (idx != -1 && title != null) {
            tabInfos[idx].title = title
            spinnerAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
        webViews.forEach { it.destroy() }
    }
}