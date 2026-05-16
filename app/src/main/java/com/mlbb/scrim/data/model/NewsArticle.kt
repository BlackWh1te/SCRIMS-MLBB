package com.mlbb.scrim.data.model

data class NewsArticle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val url: String = "",
    val imageUrl: String = "",
    val source: String = "",
    val publishedAt: Long = System.currentTimeMillis(),
    val originalLanguage: String = "en",
    val isTranslated: Boolean = false
)
