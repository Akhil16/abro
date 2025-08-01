package com.example.abro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BookmarksActivity : AppCompatActivity() {
    
    private lateinit var bookmarksList: ListView
    private lateinit var backBtn: Button
    private lateinit var emptyView: TextView
    private lateinit var bookmarkManager: BookmarkManager
    private lateinit var adapter: BookmarkAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        
        bookmarkManager = BookmarkManager(this)
        
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
        val bookmarks = bookmarkManager.getBookmarks()
        
        if (bookmarks.isEmpty()) {
            bookmarksList.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            bookmarksList.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            
            adapter = BookmarkAdapter(bookmarks)
            bookmarksList.adapter = adapter
            
            bookmarksList.setOnItemClickListener { _, _, position, _ ->
                val bookmark = bookmarks[position]
                val intent = Intent().apply {
                    putExtra("url", bookmark.url)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }
    
    private inner class BookmarkAdapter(private val bookmarks: List<Bookmark>) : BaseAdapter() {
        
        override fun getCount(): Int = bookmarks.size
        
        override fun getItem(position: Int): Bookmark = bookmarks[position]
        
        override fun getItemId(position: Int): Long = position.toLong()
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_bookmark, parent, false)
            val bookmark = bookmarks[position]
            
            val titleView = view.findViewById<TextView>(R.id.bookmark_title)
            val urlView = view.findViewById<TextView>(R.id.bookmark_url)
            val deleteBtn = view.findViewById<ImageButton>(R.id.delete_btn)
            
            titleView.text = bookmark.title
            urlView.text = bookmark.url
            
            deleteBtn.setOnClickListener {
                bookmarkManager.removeBookmark(bookmark.url)
                setupBookmarksList() // Refresh the list
            }
            
            return view
        }
    }
}