package com.example.abro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.net.URL
import androidx.core.net.toUri
import androidx.core.content.edit

// Holds info for each browser tab
data class TabInfo(
    var title: String,
    var favicon: Bitmap? = null,
    var url: String,
    var isIncognito: Boolean = false
)

class MainActivity : AppCompatActivity() {

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
    private lateinit var spinnerAdapter: ArrayAdapter<TabInfo>
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var downloadHelper: DownloadHelper

    private val gson = Gson()
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val bookmarkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val url = result.data?.getStringExtra("url")
            if (url != null) {
                getCurrentWebView()?.loadUrl(url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply night mode setting
        val prefs = getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
        val isNightMode = prefs.getBoolean("night_mode", false)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bookmarkManager = BookmarkManager(this)
        downloadHelper = DownloadHelper(this)

        // Bind views
        webViewContainer = findViewById(R.id.webview_container)
        backBtn = findViewById(R.id.btn_back)
        refreshBtn = findViewById(R.id.btn_refresh)
        urlBar = findViewById(R.id.url_bar)

        tabSpinner = findViewById(R.id.tab_spinner)
        btnAddTab = findViewById(R.id.btn_add_tab)
        btnCloseTab = findViewById(R.id.btn_close_tab)
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnMenu = findViewById(R.id.btn_menu)

        // Setup spinner adapter with custom spinner item layout for favicon + title
        spinnerAdapter = object : ArrayAdapter<TabInfo>(this, R.layout.item_tab_spinner, tabInfos) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createTabSpinnerView(position, convertView, parent)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createTabSpinnerView(position, convertView, parent)
            }
        }
        tabSpinner.adapter = spinnerAdapter

        // Load saved tabs or create a default tab
        val savedTabs = loadTabsFromPreferences()
        if (savedTabs.isNotEmpty()) {
            for ((index, url) in savedTabs.withIndex()) {
                addTab(url, switchTo = index == 0)
            }
        } else {
            addTab("https://www.google.com")
        }

        // Spinner tab selection listener
        tabSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != currentTabIndex && position in webViews.indices) {
                    switchToTab(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Add/close tab buttons
        btnAddTab.setOnClickListener {
            showTabOptions()
        }

        btnCloseTab.setOnClickListener {
            if (webViews.size <= 1) {
                Toast.makeText(this, "Cannot close the last tab.", Toast.LENGTH_SHORT).show()
            } else {
                closeTab(currentTabIndex)
            }
        }

        // Bookmark button
        btnBookmark.setOnClickListener {
            toggleBookmark()
        }

        // Menu button
        btnMenu.setOnClickListener {
            showMenu()
        }

        // Navigation buttons
        backBtn.setOnClickListener {
            getCurrentWebView()?.let {
                if (it.canGoBack()) it.goBack()
            }
        }

        refreshBtn.setOnClickListener {
            getCurrentWebView()?.reload()
        }

        // URL bar "Go" action handler
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                loadUrlFromBar()
                true
            } else false
        }

        // Back press handling with gesture support
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

    private fun toggleBookmark() {
        val currentUrl = getCurrentWebView()?.url ?: return
        val currentTitle = getCurrentWebView()?.title ?: getDomainFromUrl(currentUrl)
        
        if (bookmarkManager.isBookmarked(currentUrl)) {
            bookmarkManager.removeBookmark(currentUrl)
            Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show()
        } else {
            bookmarkManager.addBookmark(currentTitle, currentUrl)
            Toast.makeText(this, "Bookmark added", Toast.LENGTH_SHORT).show()
        }
        updateBookmarkButton()
    }

    private fun updateBookmarkButton() {
        val currentUrl = getCurrentWebView()?.url ?: return
        if (bookmarkManager.isBookmarked(currentUrl)) {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
        }
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

    private fun createTabSpinnerView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.item_tab_spinner, parent, false)
        val tabInfo = tabInfos.getOrNull(position)
        val titleView = view.findViewById<TextView>(R.id.tab_title)
        val faviconView = view.findViewById<ImageView>(R.id.tab_favicon)
        
        val displayTitle = if (tabInfo?.isIncognito == true) {
            "🕶️ ${tabInfo.title}"
        } else {
            tabInfo?.title ?: ""
        }
        titleView.text = displayTitle
        
        if (tabInfo?.favicon != null) {
            faviconView.setImageBitmap(tabInfo.favicon)
        } else {
            faviconView.setImageResource(R.drawable.ic_default_favicon) // This drawable should exist in res/drawable
        }
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun addTab(initialUrl: String, switchTo: Boolean = true, isIncognito: Boolean = false) {
        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            
            // Configure incognito mode
            if (isIncognito) {
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                clearCache(true)
                clearFormData()
                clearHistory()
            }
            
            // Set download listener
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                downloadHelper.downloadFile(url, userAgent, contentDisposition, mimeType)
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    val idx = webViews.indexOf(this@apply)
                    if (icon != null && idx != -1) {
                        tabInfos[idx].favicon = icon
                        spinnerAdapter.notifyDataSetChanged()
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val idx = webViews.indexOf(this@apply)
                    if (idx != -1) {
                        tabInfos[idx].url = url ?: ""
                        if (currentTabIndex == idx) {
                            urlBar.setText(url)
                            updateNavButtons()
                            updateBookmarkButton()
                        }
                        // Update tab title
                        val title = view?.title ?: getDomainFromUrl(url ?: "")
                        tabInfos[idx].title = title
                        spinnerAdapter.notifyDataSetChanged()
                        updateTabTitles()
                        fetchFavicon(getDomainFromUrl(url ?: ""), idx) // fallback
                    }
                }
            }
            loadUrl(initialUrl)
        }
        webViews.add(webView)
        tabInfos.add(
            TabInfo(
                title = getDomainFromUrl(initialUrl),
                favicon = null,
                url = initialUrl,
                isIncognito = isIncognito
            )
        )
        spinnerAdapter.notifyDataSetChanged()
        saveTabsToPreferences()
        if (switchTo) switchToTab(webViews.size - 1)
    }

    private fun fetchFavicon(domain: String, tabIndex: Int) {
        uiScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://$domain/favicon.ico")
                    BitmapFactory.decodeStream(url.openConnection().getInputStream())
                } catch (e: Exception) {
                    Log.d("FaviconFetch", "Failed to load favicon for $domain: ${e.message}")
                    null
                }
            }
            if (bmp != null && tabIndex in tabInfos.indices) {
                tabInfos[tabIndex].favicon = bmp
                spinnerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun switchToTab(index: Int) {
        if (index !in webViews.indices) return
        webViewContainer.removeAllViews()
        webViewContainer.addView(webViews[index])
        currentTabIndex = index
        urlBar.setText(webViews[index].url)
        updateNavButtons()
        updateBookmarkButton()
        updateTabTitles()
        if (tabSpinner.selectedItemPosition != index) {
            tabSpinner.setSelection(index)
        }
        saveTabsToPreferences()
    }

    private fun closeTab(index: Int) {
        if (index !in webViews.indices) return

        webViewContainer.removeView(webViews[index])
        webViews.removeAt(index)
        tabInfos.removeAt(index)
        spinnerAdapter.notifyDataSetChanged()

        if (webViews.isEmpty()) {
            addTab("https://www.google.com", true)
            return
        }
        val newIndex = when {
            index == 0 -> 0
            else -> index - 1
        }
        switchToTab(newIndex)
        saveTabsToPreferences()
    }

    private fun getCurrentWebView(): WebView? = webViews.getOrNull(currentTabIndex)

    private fun loadUrlFromBar() {
        var url = urlBar.text.toString().trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://www.google.com/search?q=" + url.replace(" ", "+")
        }
        getCurrentWebView()?.loadUrl(url)
    }

    private fun updateNavButtons() {
        backBtn.isEnabled = getCurrentWebView()?.canGoBack() == true
        refreshBtn.isEnabled = getCurrentWebView() != null
    }

    private fun updateTabTitles() {
        if (currentTabIndex !in tabInfos.indices) return
        tabInfos[currentTabIndex].title = webViews[currentTabIndex].title
            ?: getDomainFromUrl(webViews[currentTabIndex].url ?: "")
        spinnerAdapter.notifyDataSetChanged()
    }

    private fun getDomainFromUrl(url: String): String {
        return try {
            val uri = url.toUri()
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun saveTabsToPreferences() {
        val prefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        val urls = webViews.map { it.url ?: "" }
        val json = gson.toJson(urls)
        prefs.edit { putString("tabs_json", json) }
    }

    private fun loadTabsFromPreferences(): List<String> {
        val prefs = getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("tabs_json", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel() // cancel coroutines cleanly
        webViews.forEach { it.destroy() }
    }
}
