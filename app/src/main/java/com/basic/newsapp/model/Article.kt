package com.basic.newsapp.model

data class Article(
    val guid: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val content: String = "",
    val link: String = "",
    val isVisited: Boolean = false,
    val isFavorite: Boolean = false
)
