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
    val addedAt: Long
)
