package com.sabir.watchtracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ContinueWatchingCacheDao {

    @Query("SELECT * FROM continue_watching_cache")
    suspend fun getAll(): List<ContinueWatchingCache>

    @Upsert
    suspend fun upsert(cache: ContinueWatchingCache)

    @Query("DELETE FROM continue_watching_cache WHERE tmdbShowId = :tmdbShowId")
    suspend fun deleteForShow(tmdbShowId: Int)

    @Query("DELETE FROM continue_watching_cache WHERE tmdbShowId NOT IN (SELECT tmdbId FROM library_items WHERE mediaType = 'tv')")
    suspend fun deleteOrphans(): Int
}
