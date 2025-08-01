package com.example.abro.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.abro.R
import com.example.abro.data.models.TabInfo

class TabSpinnerAdapter(
    context: Context,
    private val tabInfos: MutableList<TabInfo>
) : ArrayAdapter<TabInfo>(context, R.layout.item_tab_spinner, tabInfos) {
    
    private val inflater = LayoutInflater.from(context)
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createTabSpinnerView(position, convertView, parent)
    }
    
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createTabSpinnerView(position, convertView, parent)
    }
    
    private fun createTabSpinnerView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_tab_spinner, parent, false)
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
            faviconView.setImageResource(R.drawable.ic_default_favicon)
        }
        return view
    }
}