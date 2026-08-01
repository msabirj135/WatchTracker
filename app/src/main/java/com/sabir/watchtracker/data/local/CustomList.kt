package com.sabir.watchtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val colorKey: String? = null,
    val iconKey: String? = null
)
