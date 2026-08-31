package org.jethavanarama.dhammasermons.data.model

data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String,
    val articleUrl: String,
    val dateText: String? = null,
    val fullContent: String? = null
)
