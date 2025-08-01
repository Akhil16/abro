package com.example.abro.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object FaviconLoader {
    
    suspend fun loadFavicon(domain: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://$domain/favicon.ico")
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } catch (e: Exception) {
                Log.d("FaviconLoader", "Failed to load favicon for $domain: ${e.message}")
                null
            }
        }
    }
}