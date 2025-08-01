package com.example.abro.utils

import androidx.core.net.toUri

object UrlUtils {
    
    fun getDomainFromUrl(url: String): String {
        return try {
            val uri = url.toUri()
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
    
    fun formatUrlForSearch(input: String): String {
        var url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://www.google.com/search?q=" + url.replace(" ", "+")
        }
        return url
    }
    
    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = url.toUri()
            uri.scheme != null && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
}