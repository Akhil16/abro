package com.example.abro.data.models

import android.graphics.Bitmap

data class TabInfo(
    var title: String,
    var favicon: Bitmap? = null,
    var url: String,
    var isIncognito: Boolean = false
)