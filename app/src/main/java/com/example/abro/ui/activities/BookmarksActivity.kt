package com.example.abro.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.abro.R
import com.example.abro.data.repositories.BookmarkRepository
import com.example.abro.ui.adapters.BookmarkAdapter

class BookmarksActivity : AppCompatActivity() {
    
    private lateinit var bookmarksList: ListView
    private lateinit var backBtn: Button
    private lateinit var emptyView: TextView
    private lateinit var bookmarkRepository: BookmarkRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        
        bookmarkRepository = BookmarkRepository(this)
        
        initViews()
        setupBookmarksList()
    }
    
    private fun initViews() {
        bookmarksList = findViewById(R.id.bookmarks_list)
        backBtn = findViewById(R.id.back_btn)
        emptyView = findViewById(R.id.empty_view)
        
        backBtn.setOnClickListener { finish() }
    }
    
    private fun setupBookmarksList() {
        val bookmarks = bookmarkRepository.getBookmarks()
        
        if (bookmarks.isEmpty()) {
            bookmarksList.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            bookmarksList.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            
            val adapter = BookmarkAdapter(
                bookmarks = bookmarks,
                onBookmarkClick = { bookmark ->
                    val intent = Intent().apply {
                        putExtra("url", bookmark.url)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                },
                onDeleteClick = { bookmark ->
                    bookmarkRepository.removeBookmark(bookmark.url)
                    setupBookmarksList() // Refresh the list
                }
            )
            
            bookmarksList.adapter = adapter
        }
    }
}