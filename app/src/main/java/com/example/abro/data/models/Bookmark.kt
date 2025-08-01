package com.example.abro.data.models

data class Bookmark(
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)