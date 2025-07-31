package com.example.abro

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
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
    var url: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var backBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var urlBar: EditText

    private lateinit var tabSpinner: Spinner
    private lateinit var btnAddTab: ImageButton
    private lateinit var btnCloseTab: ImageButton

    private val webViews = mutableListOf<WebView>()
    private val tabInfos = mutableListOf<TabInfo>()
    private var currentTabIndex = 0
    private lateinit var spinnerAdapter: ArrayAdapter<TabInfo>

    private val gson = Gson()
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        webViewContainer = findViewById(R.id.webview_container)
        backBtn = findViewById(R.id.btn_back)
        refreshBtn = findViewById(R.id.btn_refresh)
        urlBar = findViewById(R.id.url_bar)

        tabSpinner = findViewById(R.id.tab_spinner)
        btnAddTab = findViewById(R.id.btn_add_tab)
        btnCloseTab = findViewById(R.id.btn_close_tab)

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
            addTab("https://www.google.com")
        }

        btnCloseTab.setOnClickListener {
            if (webViews.size <= 1) {
                Toast.makeText(this, "Cannot close the last tab.", Toast.LENGTH_SHORT).show()
            } else {
                closeTab(currentTabIndex)
            }
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

    private fun createTabSpinnerView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: layoutInflater.inflate(R.layout.item_tab_spinner, parent, false)
        val tabInfo = tabInfos.getOrNull(position)
        val titleView = view.findViewById<TextView>(R.id.tab_title)
        val faviconView = view.findViewById<ImageView>(R.id.tab_favicon)
        titleView.text = tabInfo?.title ?: ""
        if (tabInfo?.favicon != null) {
            faviconView.setImageBitmap(tabInfo.favicon)
        } else {
            faviconView.setImageResource(R.drawable.ic_default_favicon) // This drawable should exist in res/drawable
        }
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun addTab(initialUrl: String, switchTo: Boolean = true) {
        val webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

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
                url = initialUrl
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
