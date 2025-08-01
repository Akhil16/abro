package com.example.abro

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Bookmark(
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BookmarkManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun addBookmark(title: String, url: String) {
        val bookmarks = getBookmarks().toMutableList()
        val bookmark = Bookmark(title, url)
        bookmarks.add(0, bookmark) // Add to beginning
        saveBookmarks(bookmarks)
    }
    
    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().filter { it.url != url }
        saveBookmarks(bookmarks)
    }
    
    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }
    
    fun getBookmarks(): List<Bookmark> {
        val json = prefs.getString("bookmarks_json", null) ?: return emptyList()
        val type = object : TypeToken<List<Bookmark>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveBookmarks(bookmarks: List<Bookmark>) {
        val json = gson.toJson(bookmarks)
        prefs.edit().putString("bookmarks_json", json).apply()
    }
}