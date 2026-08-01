package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {
    @Query("SELECT * FROM custom_lists")
    suspend fun getListsSnapshot(): List<CustomList>

    @Query("SELECT * FROM custom_list_items")
    suspend fun getItemsSnapshot(): List<CustomListItem>

    @Query("SELECT * FROM custom_lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<CustomList>>

    @Query("SELECT * FROM custom_list_items ORDER BY addedAt DESC")
    fun observeItems(): Flow<List<CustomListItem>>

    @Upsert
    suspend fun upsertList(list: CustomList)

    @Upsert
    suspend fun upsertLists(lists: List<CustomList>)

    @Insert
    suspend fun insertList(list: CustomList): Long

    @Upsert
    suspend fun upsertItem(item: CustomListItem)

    @Upsert
    suspend fun upsertItems(items: List<CustomListItem>)

    @Query("DELETE FROM custom_list_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM custom_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM custom_list_items WHERE listId = :listId AND tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun removeItem(listId: Long, tmdbId: Int, mediaType: String)

    @Query("DELETE FROM custom_list_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun removeTitleFromAllLists(tmdbId: Int, mediaType: String)

    @Query("DELETE FROM custom_list_items WHERE listId = :listId")
    suspend fun deleteItems(listId: Long)

    @Query("DELETE FROM custom_lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM custom_list_items WHERE listId NOT IN (SELECT id FROM custom_lists) OR NOT EXISTS (SELECT 1 FROM library_items WHERE library_items.tmdbId = custom_list_items.tmdbId AND library_items.mediaType = custom_list_items.mediaType)")
    suspend fun deleteOrphanItems(): Int
}
