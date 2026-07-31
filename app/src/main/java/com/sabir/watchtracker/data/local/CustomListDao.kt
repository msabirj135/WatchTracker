package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {
    @Query("SELECT * FROM custom_lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<CustomList>>

    @Query("SELECT * FROM custom_list_items ORDER BY addedAt DESC")
    fun observeItems(): Flow<List<CustomListItem>>

    @Upsert
    suspend fun upsertList(list: CustomList)

    @Upsert
    suspend fun upsertItem(item: CustomListItem)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun removeItem(listId: Long, tmdbId: Int, mediaType: String)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId")
    suspend fun deleteItems(listId: Long)

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)
}
