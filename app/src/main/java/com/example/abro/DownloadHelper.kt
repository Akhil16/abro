package com.example.abro

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast

class DownloadHelper(private val context: Context) {
    
    fun downloadFile(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            
            // Set download destination
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            
            // Set request properties
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(fileName)
            request.setDescription("Downloading file...")
            
            if (userAgent != null) {
                request.addRequestHeader("User-Agent", userAgent)
            }
            
            // Start download
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            
            Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}