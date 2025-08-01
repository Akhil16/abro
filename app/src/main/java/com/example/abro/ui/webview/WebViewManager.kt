package com.example.abro.ui.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.example.abro.data.models.TabInfo
import com.example.abro.utils.DownloadHelper
import com.example.abro.utils.FaviconLoader
import com.example.abro.utils.UrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WebViewManager(
    private val context: Context,
    private val downloadHelper: DownloadHelper,
    private val coroutineScope: CoroutineScope
) {
    
    interface WebViewListener {
        fun onPageFinished(webView: WebView, url: String?)
        fun onFaviconReceived(webView: WebView, icon: Bitmap?)
        fun onTitleChanged(webView: WebView, title: String?)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(
        initialUrl: String,
        isIncognito: Boolean = false,
        listener: WebViewListener? = null
    ): WebView {
        return WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Configure WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                
                if (isIncognito) {
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    clearCache(true)
                    clearFormData()
                    clearHistory()
                }
            }
            
            // Set download listener
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                downloadHelper.downloadFile(url, userAgent, contentDisposition, mimeType)
            }
            
            // Set WebChromeClient for favicon and title updates
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    listener?.onFaviconReceived(this@apply, icon)
                }
                
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    listener?.onTitleChanged(this@apply, title)
                }
            }
            
            // Set WebViewClient for page events
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    listener?.onPageFinished(this@apply, url)
                    
                    // Load favicon as fallback
                    url?.let { urlString ->
                        val domain = UrlUtils.getDomainFromUrl(urlString)
                        coroutineScope.launch {
                            val favicon = FaviconLoader.loadFavicon(domain)
                            favicon?.let { listener?.onFaviconReceived(this@apply, it) }
                        }
                    }
                }
            }
            
            loadUrl(initialUrl)
        }
    }
    
    fun configureIncognitoMode(webView: WebView) {
        webView.apply {
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            clearCache(true)
            clearFormData()
            clearHistory()
        }
    }
}