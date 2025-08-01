package com.example.abro.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.example.abro.R
import com.example.abro.data.models.Bookmark

class BookmarkAdapter(
    private val bookmarks: List<Bookmark>,
    private val onBookmarkClick: (Bookmark) -> Unit,
    private val onDeleteClick: (Bookmark) -> Unit
) : BaseAdapter() {
    
    override fun getCount(): Int = bookmarks.size
    
    override fun getItem(position: Int): Bookmark = bookmarks[position]
    
    override fun getItemId(position: Int): Long = position.toLong()
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        
        val bookmark = bookmarks[position]
        
        val titleView = view.findViewById<TextView>(R.id.bookmark_title)
        val urlView = view.findViewById<TextView>(R.id.bookmark_url)
        val deleteBtn = view.findViewById<ImageButton>(R.id.delete_btn)
        
        titleView.text = bookmark.title
        urlView.text = bookmark.url
        
        view.setOnClickListener { onBookmarkClick(bookmark) }
        deleteBtn.setOnClickListener { onDeleteClick(bookmark) }
        
        return view
    }
}