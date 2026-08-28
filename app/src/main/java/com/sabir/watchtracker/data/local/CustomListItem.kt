package com.sabir.watchtracker.data.local

import androidx.room.Entity

@Entity(
    tableName = "custom_list_items",
    primaryKeys = ["listId", "tmdbId", "mediaType"]
)
data class CustomListItem(
    val listId: Long,
    val tmdbId: Int,
    val mediaType: String,
    val addedAt: Long,
    val title: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val originalTitle: String? = null,
    val originalLanguage: String? = null,
    val tmdbRating: Double? = null
)
